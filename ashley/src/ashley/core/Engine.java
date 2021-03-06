package ashley.core;

import ashley.signals.Listener;
import ashley.signals.Signal;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;

import java.util.Comparator;

/**
 * The Engine class is the heart of the Entity framework. It is responsible for keeping track of entities and
 * managing EntitySystems. The Engine should be updated every tick via the update() method.
 *
 * With the Engine you can:
 *
 * - Add/Remove Entities
 * - Add/Remove EntitySystems
 * - Obtain a list of entities for a specific Family
 * - Update the main loop
 *
 * @author Stefan Bachmann
 */
public class Engine {
	private static final SystemComparator comparator = new SystemComparator();

	/** An unordered array that holds all entities in the Engine */
	private final Array<Entity> entities;
	/** An unordered list of EntitySystem */
	private final Array<EntitySystem> systems;
	/** A hashmap that organises EntitySystems by class for easy retrieval */
	private final ObjectMap<Class<?>, EntitySystem> systemsByClass;
	/** A hashmap that organises all entities into family buckets */
	private final ObjectMap<Family, IntMap<Entity>> families;

	/** A listener for the Engine that's called everytime a component is added. */
	private final Listener<Entity> componentAdded;
	/** A listener for the Engine that's called everytime a component is removed. */
	private final Listener<Entity> componentRemoved;

	public Engine(){
		entities = new Array<Entity>();
		systems = new Array<EntitySystem>();
		systemsByClass = new ObjectMap<Class<?>, EntitySystem>();
		families = new ObjectMap<Family, IntMap<Entity>>();

		componentAdded = new Listener<Entity>(){
			@Override
			public void receive(Signal<Entity> signal, Entity object) {
				componentAdded(object);
			}
		};

		componentRemoved = new Listener<Entity>(){
			@Override
			public void receive(Signal<Entity> signal, Entity object) {
				componentRemoved(object);
			}
		};
	}

	/**
	 * Add an entity to this Engine
	 * @param entity The Entity to add
	 */
	public void addEntity(Entity entity){
		entities.add(entity);

		for(Entry<Family, IntMap<Entity>> entry: families.entries()) {
			if(entry.key.matches(entity)) {
				entry.value.put(entity.getIndex(), entity);
				entity.getFamilyBits().set(entry.key.getFamilyIndex());
			}
		}

		entity.componentAdded.add(componentAdded);
		entity.componentRemoved.add(componentRemoved);
	}

	/**
	 * Remove an entity from this Engine
	 * @param entity The Entity to remove
	 */
	public void removeEntity(Entity entity){
		entities.removeValue(entity, true);

		if(!entity.getFamilyBits().isEmpty()){
			for(Entry<Family, IntMap<Entity>> entry : families.entries()) {
				if(entry.key.matches(entity)){
					entry.value.remove(entity.getIndex());
					entity.getFamilyBits().clear(entry.key.getFamilyIndex());
				}
			}
		}

		entity.componentAdded.remove(componentAdded);
		entity.componentRemoved.remove(componentRemoved);
	}

	/**
	 * Add the EntitySystem to this Engine
	 * @param system The system to add
	 */
	public void addSystem(EntitySystem system){
		Class<? extends EntitySystem> systemType = system.getClass();

		if (!systemsByClass.containsKey(systemType)) {
			systems.add(system);
			systemsByClass.put(systemType, system);
			system.addedToEngine(this);

			systems.sort(comparator);
		}
	}

	/**
	 * Removes the EntitySystem from this Engine
	 * @param system The system to remove
	 */
	public void removeSystem(EntitySystem system){
		if(systems.removeValue(system, true))
			system.removedFromEngine(this);
	}

	/**
	 * Quick entity system retrieval
	 * @param systemType The EntitySystem class to retrieve
	 * @return The Entity System
	 */
	public <T extends EntitySystem> T getSystem(Class<T> systemType) {
		return systemType.cast(systemsByClass.get(systemType));
	}

	/**
	 * Returns an IntMap of entities for the specified Family. Will return the same instance every time.
	 * @param family The Family
	 * @return An IntMap of Entities
	 */
	public IntMap<Entity> getEntitiesFor(Family family){
		if(families.get(family, null) == null){
			IntMap<Entity> entityIntMap = new IntMap<Entity>();
			for(Entity e:this.entities){
				if(family.matches(e))
					entityIntMap.put(e.getIndex(), e);
			}
			families.put(family, entityIntMap);
		}
		return families.get(family);
	}

	/**
	 * Internal listener for when a Component is added to an entity
	 * @param entity The Entity that had a component added to
	 */
	private void componentAdded(Entity entity){
		for(Entry<Family, IntMap<Entity>> entry : families.entries()) {
			if(!entity.getFamilyBits().get(entry.key.getFamilyIndex())){
				if (entry.key.matches(entity)) {
					entry.value.put(entity.getIndex(), entity);
					entity.getFamilyBits().set(entry.key.getFamilyIndex());
				}
			}
		}
	}


	/**
	 * Internal listener for when a Component is removed from an entity
	 * @param entity The Entity that had a component removed from
	 */
	private void componentRemoved(Entity entity){
		for(Entry<Family, IntMap<Entity>> entry : families.entries()) {
			if(entity.getFamilyBits().get(entry.key.getFamilyIndex())) {
				if (!entry.key.matches(entity)) {
					entry.value.remove(entity.getIndex());
					entity.getFamilyBits().clear(entry.key.getFamilyIndex());
				}
			}
		}
	}

	/**
	 * Updates all the systems in this Engine
	 * @param deltaTime The time passed since the last frame
	 */
	public void update(float deltaTime){
		for(EntitySystem system : systems) {
			system.update(deltaTime);
		}
	}

	private static class SystemComparator implements Comparator<EntitySystem>{
		@Override
		public int compare(EntitySystem a, EntitySystem b) {
			return a.priority > b.priority ? 1 : (a.priority == b.priority) ? 0 : -1;
		}
	}
}
