package com.gadarts.industrial.systems.character.commands;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pools;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.CharacterComponent;
import com.gadarts.industrial.components.character.OnGoingAttack;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.model.characters.enemies.WeaponsDefinitions;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.CharacterSystemEventsSubscriber;
import com.gadarts.industrial.utils.GameUtils;

import java.util.List;

public class PrimaryAttackCharacterCommand extends CharacterCommand {
	private final static Vector3 auxVector3_1 = new Vector3();

	private final static Vector3 auxVector3_2 = new Vector3();

	private static int randomNumberOfBullets(WeaponsDefinitions primary) {
		return MathUtils.random(primary.getMinNumberOfBullets(), primary.getMaxNumberOfBullets());
	}

	@Override
	public void reset( ) {

	}

	@Override
	public boolean initialize(Entity character,
							  SystemsCommonData commonData,
							  Object additionalData,
							  List<CharacterSystemEventsSubscriber> subscribers) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		if (characterComponent.getTarget() != null) {
			characterComponent.getRotationData().setRotating(true);
		}
		CharacterComponent charComp = ComponentsMapper.character.get(character);
		WeaponsDefinitions primary = charComp.getPrimaryAttack();
		int bulletsToShoot = primary.isMelee() ? 1 : randomNumberOfBullets(primary);
		charComp.getOnGoingAttack().initialize(CharacterComponent.AttackType.PRIMARY, bulletsToShoot);
		return false;
	}

	@Override
	public boolean reactToFrameChange(SystemsCommonData systemsCommonData,
									  Entity character,
									  TextureAtlas.AtlasRegion newFrame,
									  List<CharacterSystemEventsSubscriber> subscribers) {
		return engagePrimaryAttack(character, newFrame, systemsCommonData, subscribers);
	}

	@Override
	public void free( ) {
		Pools.get(PrimaryAttackCharacterCommand.class).free(this);
	}

	private Vector3 calculateDirectionToTarget(CharacterComponent characterComp,
											   Vector3 positionNodeCenterPosition,
											   SystemsCommonData commonData) {
		CharacterDecalComponent targetDecalComp = ComponentsMapper.characterDecal.get(characterComp.getTarget());
		MapGraphNode targetNode = commonData.getMap().getNode(targetDecalComp.getDecal().getPosition());
		Vector3 targetNodeCenterPosition = targetNode.getCenterPosition(auxVector3_2);
		targetNodeCenterPosition.y += 0.5f;
		return targetNodeCenterPosition.sub(positionNodeCenterPosition);
	}

	private boolean engagePrimaryAttack(Entity character,
										TextureAtlas.AtlasRegion newFrame,
										SystemsCommonData commonData,
										List<CharacterSystemEventsSubscriber> subscribers) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		OnGoingAttack onGoingAttack = characterComponent.getOnGoingAttack();
		if (onGoingAttack.isDone()) return false;

		int primaryAttackHitFrameIndex = GameUtils.getPrimaryAttackHitFrameIndexForCharacter(character, commonData);
		boolean commandDone = false;
		if (newFrame.index == primaryAttackHitFrameIndex) {
			CharacterDecalComponent charDecalComp = ComponentsMapper.characterDecal.get(character);
			MapGraphNode positionNode = commonData.getMap().getNode(charDecalComp.getDecal().getPosition());
			Vector3 positionNodeCenterPosition = positionNode.getCenterPosition(auxVector3_1);
			Vector3 direction = calculateDirectionToTarget(characterComponent, positionNodeCenterPosition, commonData);
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onCharacterEngagesPrimaryAttack(character, direction, positionNodeCenterPosition);
			}
			onGoingAttack.bulletShot();
			if (onGoingAttack.getBulletsToShoot() <= 0) {
				WeaponsDefinitions primaryAttack = characterComponent.getPrimaryAttack();
				consumeTurnTime(character, primaryAttack.getDuration());
				commandDone = primaryAttack.isMelee();
			}
		}
		return commandDone;
	}

}
