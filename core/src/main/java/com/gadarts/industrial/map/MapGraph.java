package com.gadarts.industrial.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.shared.model.Coords;
import com.gadarts.industrial.shared.model.map.MapNodesTypes;
import lombok.Getter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.gadarts.industrial.components.character.CharacterComponent.PASSABLE_MAX_HEIGHT_DIFF;

public class MapGraph implements IndexedGraph<MapGraphNode> {
	private static final Vector3 auxVector3 = new Vector3();
	private static final Array<Connection<MapGraphNode>> auxConnectionsList = new Array<>();
	private final static Vector2 auxVector2 = new Vector2();
	private static final List<MapGraphNode> auxNodesList_1 = new ArrayList<>();
	private static final List<MapGraphNode> auxNodesList_2 = new ArrayList<>();
	@Getter
	private final float ambient;
	private final Dimension mapSize;
	@Getter
	private final Array<MapGraphNode> nodes;
	private final MapGraphRelatedEntities mapGraphRelatedEntities = new MapGraphRelatedEntities();
	@Getter
	private final MapGraphStates mapGraphStates = new MapGraphStates();

	public MapGraph(Dimension mapSize, PooledEngine engine, float ambient) {
		this.ambient = ambient;
		mapGraphRelatedEntities.init(engine);
		this.mapSize = mapSize;
		this.nodes = new Array<>(mapSize.width * mapSize.height);
		for (int row = 0; row < mapSize.height; row++) {
			for (int col = 0; col < mapSize.width; col++) {
				nodes.add(new MapGraphNode(col, row, MapNodesTypes.values()[MapNodesTypes.PASSABLE_NODE.ordinal()], 8));
			}
		}
	}

	public Entity fetchAliveCharacterFromNode(MapGraphNode node) {
		return fetchAliveCharacterFromNode(node, false);
	}

	public Entity fetchAliveCharacterFromNode(MapGraphNode node, boolean includePlayer) {
		for (Entity character : mapGraphRelatedEntities.getCharacterEntities()) {
			if (!ComponentsMapper.player.has(character) || includePlayer) {
				Decal decal = ComponentsMapper.characterDecal.get(character).getDecal();
				MapGraphNode characterNode = getNode(decal.getPosition());
				int hp = ComponentsMapper.character.get(character).getSkills().getHealthData().getHp();
				if (hp > 0 && characterNode.equals(node)) {
					return character;
				}
			}
		}
		return null;
	}

	private void getThreeBehind(final MapGraphNode node, final List<MapGraphNode> output) {
		int x = node.getCol();
		int y = node.getRow();
		if (y > 0) {
			if (x > 0) {
				output.add(getNode(x - 1, y - 1));
			}
			output.add(getNode(x, y - 1));
			if (x < mapSize.width - 1) {
				output.add(getNode(x + 1, y - 1));
			}
		}
	}

	private void getThreeInFront(final MapGraphNode node, final List<MapGraphNode> output) {
		int x = node.getCol();
		int y = node.getRow();
		if (y < mapSize.height - 1) {
			if (x > 0) {
				output.add(getNode(x - 1, y + 1));
			}
			output.add(getNode(x, y + 1));
			if (x < mapSize.width - 1) {
				output.add(getNode(x + 1, y + 1));
			}
		}
	}

	public java.util.List<MapGraphNode> getNodesAround(final MapGraphNode node, final List<MapGraphNode> output) {
		output.clear();
		getThreeBehind(node, output);
		getThreeInFront(node, output);
		if (node.getCol() > 0) {
			output.add(getNode(node.getCol() - 1, node.getRow()));
		}
		if (node.getCol() < mapSize.width - 1) {
			output.add(getNode(node.getCol() + 1, node.getRow()));
		}
		return output;
	}

	public Entity getPickupFromNode(final MapGraphNode node) {
		Entity result = null;
		for (Entity pickup : mapGraphRelatedEntities.getPickupEntities()) {
			ModelInstance modelInstance = ComponentsMapper.modelInstance.get(pickup).getModelInstance();
			MapGraphNode pickupNode = getNode(modelInstance.transform.getTranslation(auxVector3));
			if (pickupNode.equals(node)) {
				result = pickup;
				break;
			}
		}
		return result;
	}

	public MapGraphNode getNode(final Vector3 position) {
		return getNode((int) position.x, (int) position.z);
	}

	public MapGraphNode getNode(final int col, final int row) {
		if (col < 0 || col >= getWidth() || row < 0 || row >= getDepth()) return null;

		int index = row * mapSize.width + col;
		MapGraphNode result = null;
		if (0 <= index && index < getWidth() * getDepth()) {
			result = nodes.get(index);
		}
		return result;
	}

	public int getDepth( ) {
		return mapSize.height;
	}

	public int getWidth( ) {
		return mapSize.width;
	}

	public MapGraphNode getNode(final Vector2 position) {
		return getNode((int) position.x, (int) position.y);
	}

	@Override
	public int getIndex(MapGraphNode node) {
		return node.getIndex(mapSize);
	}

	@Override
	public int getNodeCount( ) {
		return nodes.size;
	}

	@Override
	public Array<Connection<MapGraphNode>> getConnections(MapGraphNode fromNode) {
		auxConnectionsList.clear();
		Array<MapGraphConnection> connections = fromNode.getConnections();
		for (Connection<MapGraphNode> connection : connections) {
			checkIfConnectionIsAvailable(connection);
		}
		return auxConnectionsList;
	}

	public boolean checkIfNodeIsFreeOfAliveCharacters(MapGraphNode destinationNode) {
		return checkIfNodeIsFreeOfAliveCharacters(destinationNode, null);
	}

	public boolean checkIfNodeIsFreeOfCharacters(MapGraphNode destinationNode) {
		return checkIfNodeIsFreeOfCharactersAndClosedDoors(
				destinationNode,
				null,
				false,
				false);
	}

	public boolean checkIfNodeIsFreeOfAliveCharacters(MapGraphNode destinationNode, MapGraphNode pathFinalNode) {
		return checkIfNodeIsFreeOfCharactersAndClosedDoors(destinationNode, pathFinalNode, false, true);
	}

	public boolean checkIfNodeIsFreeOfAliveCharactersAndClosedDoors(GridPoint2 destinationNode) {
		MapGraphNode node = getNode(destinationNode);
		return checkIfNodeIsFreeOfCharactersAndClosedDoors(node, null, true, true);
	}

	public boolean checkIfNodeIsFreeOfCharactersAndClosedDoors(MapGraphNode destinationNode,
															   MapGraphNode pathFinalNode,
															   boolean includeClosedDoors,
															   boolean alive) {
		Entity door = destinationNode.getDoor();
		if (pathFinalNode != null && pathFinalNode.equals(destinationNode)) return true;
		if (includeClosedDoors
				&& door != null
				&& ComponentsMapper.door.get(door).getState() != DoorComponent.DoorStates.OPEN) return false;

		for (Entity c : mapGraphRelatedEntities.getCharacterEntities()) {
			MapGraphNode node = getNode(ComponentsMapper.characterDecal.get(c).getNodePosition(auxVector2));
			int hp = ComponentsMapper.character.get(c).getSkills().getHealthData().getHp();
			if ((!alive || hp > 0) && node.equals(destinationNode)) {
				return false;
			}
		}

		return true;
	}

	private boolean isNodeRevealed(MapGraphNode node) {
		return node.getEntity() != null && ComponentsMapper.modelInstance.get(node.getEntity()).getFlatColor() == null;
	}

	private void checkIfConnectionIsAvailable(final Connection<MapGraphNode> connection) {
		boolean available = true;
		if (mapGraphStates.isIncludeCharactersInGetConnections()) {
			MapGraphNode currentPathFinalDestination = mapGraphStates.getCurrentPathFinalDestination();
			available = checkIfNodeIsFreeOfAliveCharacters(connection.getToNode(), currentPathFinalDestination);
		}
		boolean validCost = connection.getCost() <= mapGraphStates.getMaxConnectionCostInSearch().getCostValue();
		if (available && validCost && checkIfConnectionPassable(connection)) {
			auxConnectionsList.add(connection);
		}
	}

	public MapGraphConnection findConnection(MapGraphNode node1, MapGraphNode node2) {
		if (node1 == null || node2 == null) return null;
		MapGraphConnection result = findConnectionBetweenTwoNodes(node1, node2);
		if (result == null) {
			result = findConnectionBetweenTwoNodes(node2, node1);
		}
		return result;
	}

	public List<MapGraphNode> fetchAvailableNodesAroundNode(final MapGraphNode node) {
		auxNodesList_1.clear();
		auxNodesList_2.clear();
		List<MapGraphNode> nodesAround = getNodesAround(node, auxNodesList_1);
		List<MapGraphNode> availableNodes = auxNodesList_2;

		for (MapGraphNode nearbyNode : nodesAround) {
			Entity character = fetchAliveCharacterFromNode(nearbyNode, true);
			if (nearbyNode.getType() == MapNodesTypes.PASSABLE_NODE && character == null) {
				availableNodes.add(nearbyNode);
			}
		}

		return availableNodes;
	}

	private MapGraphConnection findConnectionBetweenTwoNodes(MapGraphNode src, MapGraphNode dst) {
		Array<MapGraphConnection> connections = src.getConnections();
		for (MapGraphConnection connection : connections) {
			if (connection.getToNode() == dst) {
				return connection;
			}
		}
		return null;
	}

	private boolean checkIfConnectionPassable(final Connection<MapGraphNode> con) {
		if (mapGraphStates.getCurrentCharacterPathPlanner() != null
				&& ComponentsMapper.player.has(mapGraphStates.getCurrentCharacterPathPlanner())
				&& !isNodeRevealed(con.getToNode()))
			return false;

		MapGraphNode fromNode = con.getFromNode();
		MapGraphNode toNode = con.getToNode();
		boolean result = fromNode.getType() == MapNodesTypes.PASSABLE_NODE && toNode.getType() == MapNodesTypes.PASSABLE_NODE;
		result &= Math.abs(fromNode.getCol() - toNode.getCol()) < 2 && Math.abs(fromNode.getRow() - toNode.getRow()) < 2;
		if ((fromNode.getCol() != toNode.getCol()) && (fromNode.getRow() != toNode.getRow())) {
			result &= getNode(fromNode.getCol(), toNode.getRow()).getType() != MapNodesTypes.OBSTACLE_KEY_DIAGONAL_FORBIDDEN;
			result &= getNode(toNode.getCol(), fromNode.getRow()).getType() != MapNodesTypes.OBSTACLE_KEY_DIAGONAL_FORBIDDEN;
		}
		return result;
	}

	public MapGraphNode getNode(final Coords coord) {
		return getNode(coord.getCol(), coord.getRow());
	}

	public MapGraphNode getNode(GridPoint2 coord) {
		return getNode(coord.x, coord.y);
	}

	private boolean isDiagonalBlockedWithEastOrWest(final MapGraphNode source, final int col) {
		float east = getNode(col, source.getRow()).getHeight();
		return Math.abs(source.getHeight() - east) > PASSABLE_MAX_HEIGHT_DIFF;
	}

	private boolean isDiagonalBlockedWithNorthAndSouth(final MapGraphNode target,
													   final int srcX,
													   final int srcY,
													   final float srcHeight) {
		if (srcY < target.getRow()) {
			float bottom = getNode(srcX, srcY + 1).getHeight();
			return Math.abs(srcHeight - bottom) > PASSABLE_MAX_HEIGHT_DIFF;
		} else {
			float top = getNode(srcX, srcY - 1).getHeight();
			return Math.abs(srcHeight - top) > PASSABLE_MAX_HEIGHT_DIFF;
		}
	}

	private boolean isDiagonalPossible(final MapGraphNode source, final MapGraphNode target) {
		if (source.getCol() == target.getCol() || source.getRow() == target.getRow()) return true;
		if (source.getCol() < target.getCol()) {
			if (isDiagonalBlockedWithEastOrWest(source, source.getCol() + 1)) {
				return false;
			}
		} else if (isDiagonalBlockedWithEastOrWest(source, source.getCol() - 1)) {
			return false;
		}
		return !isDiagonalBlockedWithNorthAndSouth(target, source.getCol(), source.getRow(), source.getHeight());
	}

	private void addConnection(final MapGraphNode source, final int xOffset, final int yOffset) {
		MapGraphNode target = getNode(source.getCol() + xOffset, source.getRow() + yOffset);
		if (target.getType() == MapNodesTypes.PASSABLE_NODE && isDiagonalPossible(source, target)) {
			MapGraphConnection connection;
			if (Math.abs(source.getHeight() - target.getHeight()) <= PASSABLE_MAX_HEIGHT_DIFF) {
				connection = new MapGraphConnection(source, target, MapGraphConnectionCosts.CLEAN);
			} else {
				connection = new MapGraphConnection(source, target, MapGraphConnectionCosts.HEIGHT_DIFF);
			}
			source.getConnections().add(connection);
		}
	}

	void applyConnections( ) {
		for (int row = 0; row < mapSize.height; row++) {
			int rows = row * mapSize.width;
			for (int col = 0; col < mapSize.width; col++) {
				MapGraphNode n = nodes.get(rows + col);
				if (col > 0) addConnection(n, -1, 0);
				if (col > 0 && row < mapSize.height - 1) addConnection(n, -1, 1);
				if (col > 0 && row > 0) addConnection(n, -1, -1);
				if (row > 0) addConnection(n, 0, -1);
				if (row > 0 && col < mapSize.width - 1) addConnection(n, 1, -1);
				if (col < mapSize.width - 1) addConnection(n, 1, 0);
				if (col < mapSize.width - 1 && row < mapSize.height - 1) addConnection(n, 1, 1);
				if (row < mapSize.height - 1) addConnection(n, 0, 1);
			}
		}
	}

	public void init( ) {
		applyConnections();
	}

	public Entity findObstacleByNode(final MapGraphNode node) {
		Entity result = null;
		for (Entity obstacle : mapGraphRelatedEntities.getEnvironmentObjectsEntities()) {
			ModelInstance modelInstance = ComponentsMapper.modelInstance.get(obstacle).getModelInstance();
			MapGraphNode pickupNode = getNode(modelInstance.transform.getTranslation(auxVector3));
			if (pickupNode.equals(node)) {
				result = obstacle;
				break;
			}
		}
		return result;
	}

	public boolean isNodesAdjacent(MapGraphNode srcNode, MapGraphNode targetNode, float maxHeight) {
		int row = Math.abs(srcNode.getRow() - targetNode.getRow());
		int col = Math.abs(srcNode.getCol() - targetNode.getCol());
		return Math.abs(srcNode.getHeight() - targetNode.getHeight()) <= maxHeight && row <= 1 && col <= 1;
	}

	public boolean checkIfNodeIsFreeOfEnvObjects(GridPoint2 destinationNode) {
		MapGraphNode node = getNode(destinationNode);
		for (Entity entity : mapGraphRelatedEntities.getEnvironmentObjectsEntities()) {
			MapNodesTypes nodeType = ComponentsMapper.environmentObject.get(entity).getType().getNodeType();
			GameModelInstance modelInstance = ComponentsMapper.modelInstance.get(entity).getModelInstance();
			Vector3 position = modelInstance.transform.getTranslation(auxVector3);
			if (nodeType != MapNodesTypes.PASSABLE_NODE && node.equals(getNode(position))) {
				return false;
			}
		}
		return true;
	}
}

