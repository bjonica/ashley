package ashley.tests;

import ashley.core.*;
import ashley.tests.components.MovementComponent;
import ashley.tests.components.PositionComponent;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntMap.Keys;

public class BasicTest {

	public static void main(String[] args){
		PooledEngine engine = new PooledEngine();

		MovementSystem movementSystem = new MovementSystem();
		PositionSystem positionSystem = new PositionSystem();

		engine.addSystem(movementSystem);
		engine.addSystem(positionSystem);

		for(int i=0; i<10; i++){
			Entity entity = engine.createEntity();
			entity.add(new PositionComponent(10, 0));
			if(i > 5)
				entity.add(new MovementComponent(10, 2));

			engine.addEntity(entity);
		}

		log("MovementSystem has: " + movementSystem.entities.size + " entities.");
		log("PositionSystem has: " + positionSystem.entities.size + " entities.");

		for(int i=0; i<10; i++){
			engine.update(0.25f);

			if(i > 5)
				engine.removeSystem(movementSystem);
		}
	}

	public static class PositionSystem extends EntitySystem {
		public IntMap<Entity> entities;

		@Override
		public void addedToEngine(Engine engine) {
			entities = engine.getEntitiesFor(Family.getFamilyFor(PositionComponent.class));
			log("PositionSystem added to engine.");
		}

		@Override
		public void removedFromEngine(Engine engine) {
			log("PositionSystem removed from engine.");
			entities = null;
		}
	}

	public static class MovementSystem extends EntitySystem {
		public IntMap<Entity> entities;

		@Override
		public void addedToEngine(Engine engine) {
			entities = engine.getEntitiesFor(Family.getFamilyFor(PositionComponent.class, MovementComponent.class));
			log("MovementSystem added to engine.");
		}

		@Override
		public void removedFromEngine(Engine engine) {
			log("MovementSystem removed from engine.");
			entities = null;
		}

		@Override
		public void update(float deltaTime) {
			Keys keys = entities.keys();

			while(keys.hasNext){
				Entity e = entities.get(keys.next());

				PositionComponent p = e.getComponent(PositionComponent.class);
				MovementComponent m = e.getComponent(MovementComponent.class);

				p.x += m.velocityX * deltaTime;
				p.y += m.velocityY * deltaTime;
			}

			log(entities.size + " Entities updated in MovementSystem.");
		}
	}

	private static void log(String string){
		System.out.println(string);
	}
}
