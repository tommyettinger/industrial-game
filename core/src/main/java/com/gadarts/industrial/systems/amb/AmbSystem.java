package com.gadarts.industrial.systems.amb;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.DoorComponent;
import com.gadarts.industrial.components.DoorComponent.DoorStates;
import com.gadarts.industrial.components.WallComponent;
import com.gadarts.industrial.components.floor.FloorComponent;
import com.gadarts.industrial.components.mi.GameModelInstance;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.components.player.PlayerComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.env.DoorTypes;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.systems.turns.TurnsSystemEventsSubscriber;

import static com.gadarts.industrial.components.DoorComponent.DoorStates.*;

public class AmbSystem extends GameSystem<AmbSystemEventsSubscriber> implements
		CharacterSystemEventsSubscriber,
		TurnsSystemEventsSubscriber {
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final int DOOR_OPEN_DURATION = 3;
	private ImmutableArray<Entity> doorEntities;
	private ImmutableArray<Entity> wallsEntities;
	private ImmutableArray<Entity> floorsEntities;

	public AmbSystem(GameAssetsManager assetsManager,
					 GameLifeCycleHandler lifeCycleHandler) {
		super(assetsManager, lifeCycleHandler);
	}

	@Override
	public Class<AmbSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return AmbSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {

	}

	@Override
	public void onSystemReset(SystemsCommonData systemsCommonData) {
		super.onSystemReset(systemsCommonData);
		doorEntities = getEngine().getEntitiesFor(Family.all(DoorComponent.class).get());
		wallsEntities = getEngine().getEntitiesFor(Family.all(WallComponent.class).get());
		floorsEntities = getEngine().getEntitiesFor(Family.all(FloorComponent.class).get());
	}

	@Override
	public void update(float deltaTime) {
		updateDoors();
		handleFloorTilesFading();
	}

	private void handleFloorTilesFading( ) {
		for (Entity entity : floorsEntities) {
			handleFloorTileFading(entity);
		}
	}

	private void handleFloorTileFading(Entity entity) {
		ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(entity);
		GameModelInstance model = modelInstanceComponent.getModelInstance();
		BlendingAttribute blendingAttribute = (BlendingAttribute) model.materials.get(0).get(BlendingAttribute.Type);
		if (shouldFloorFadeOut(ComponentsMapper.floor.get(entity), ComponentsMapper.modelInstance.get(entity))) {
			fadeOutFloor(modelInstanceComponent, blendingAttribute);
		} else {
			blendingAttribute.opacity = Math.min(1F, blendingAttribute.opacity + 0.05F);
			modelInstanceComponent.setVisible(true);
		}
	}

	private static void fadeOutFloor(ModelInstanceComponent modelInstanceComponent, BlendingAttribute blendingAttribute) {
		if (blendingAttribute.opacity > 0F) {
			blendingAttribute.opacity = Math.max(0F, blendingAttribute.opacity - 0.05F);
		} else {
			modelInstanceComponent.setVisible(false);
		}
	}

	private boolean shouldFloorFadeOut(FloorComponent currentFloorComponent,
									   ModelInstanceComponent modelInstanceComponent) {
		SystemsCommonData data = getSystemsCommonData();
		Vector3 playerNodePos = ComponentsMapper.characterDecal.get(data.getPlayer()).getNodePosition(auxVector3_1);
		Vector3 currentFloorPos = modelInstanceComponent.getModelInstance().transform.getTranslation(auxVector3_2);
		Vector3 cameraPos = data.getCamera().position;
		float floorToCameraDist = auxVector2_2.set(currentFloorPos.x, currentFloorPos.z).dst2(cameraPos.x, cameraPos.z);
		float playerToCamDist = auxVector2_1.set(playerNodePos.x, playerNodePos.z).dst2(cameraPos.x, cameraPos.z) - 1F;
		float currentHeight = currentFloorComponent.getNode().getHeight();
		return currentHeight > data.getMap().getNode(playerNodePos).getHeight() + PlayerComponent.PLAYER_HEIGHT
				&& playerToCamDist > floorToCameraDist;
	}

	private void updateDoors( ) {
		for (Entity doorEntity : doorEntities) {
			DoorComponent doorComponent = ComponentsMapper.door.get(doorEntity);
			DoorStates state = doorComponent.getState();
			if (doorComponent.getOpenRequestor() != null) {
				doorComponent.clearOpenRequestor();
				applyDoorState(doorEntity, doorComponent, OPENING);
			}
			if (state == OPENING || state == CLOSING) {
				handleDoorAction(doorEntity, doorComponent, state == OPENING ? OPEN : CLOSED);
			}
		}
	}

	private void handleDoorAction(Entity doorEntity, DoorComponent doorComponent, DoorStates targetState) {
		GameModelInstance modelInstance = ComponentsMapper.appendixModelInstance.get(doorEntity).getModelInstance();
		Vector3 nodeCenterPosition = doorComponent.getNode().getCenterPosition(auxVector3_1);
		DoorTypes doorType = doorComponent.getDefinition().getType();
		DoorAnimation doorAnimation = DoorsAnimations.animations.get(doorType);
		if (doorAnimation.isAnimationEnded(targetState, nodeCenterPosition, doorEntity)) {
			applyDoorState(doorEntity, doorComponent, targetState);
		} else {
			doorAnimation.update(modelInstance, nodeCenterPosition, targetState);
		}
	}

	private void applyDoorState(Entity doorEntity, DoorComponent doorComponent, DoorStates newState) {
		DoorStates oldState = doorComponent.getState();
		if (oldState == newState) return;

		playDoorSound(doorComponent, newState);
		doorComponent.setState(newState);
		subscribers.forEach(s -> s.onDoorStateChanged(doorEntity, oldState, newState));
	}

	private void playDoorSound(DoorComponent doorComponent, DoorStates newState) {
		SoundPlayer soundPlayer = getSystemsCommonData().getSoundPlayer();
		DoorTypes type = doorComponent.getDefinition().getType();
		if (newState == OPENING) {
			soundPlayer.playSound(type.getOpenSound());
		} else if (newState == CLOSING) {
			soundPlayer.playSound(type.getClosedSound());
		}
	}


	@Override
	public void dispose( ) {

	}

	@Override
	public void onNewTurn(Entity entity) {
		if (ComponentsMapper.door.has(entity)) {
			DoorComponent doorComponent = ComponentsMapper.door.get(entity);
			MapGraph map = getSystemsCommonData().getMap();
			if (shouldCloseDoor(doorComponent, map)) {
				closeDoor(doorComponent, entity);
			} else {
				doorComponent.setOpenCounter(doorComponent.getOpenCounter() + 1);
				subscribers.forEach(s -> s.onDoorStayedOpenInTurn(entity));
			}
		}
	}

	private boolean shouldCloseDoor(DoorComponent doorComponent, MapGraph map) {
		int openCounter = doorComponent.getOpenCounter();
		return openCounter >= DOOR_OPEN_DURATION && map.checkIfNodeIsFreeOfCharacters(doorComponent.getNode());
	}

	private void closeDoor(DoorComponent doorComponent, Entity doorEntity) {
		doorComponent.setOpenCounter(0);
		applyDoorState(doorEntity, doorComponent, CLOSING);
	}
}
