package com.hotel.repository;

import com.hotel.interfaces.Searchable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic base repository for storing and retrieving any entity type.
 *
 * KEY CONCEPTS:
 *
 * GENERICS     — <T> is a type parameter. This ONE class works as a repository
 *                for Room, Guest, or Booking — whatever type T is filled in with.
 *
 * COLLECTIONS  — HashMap<String, T> for O(1) lookup by ID.
 *                ArrayList<T> for returning ordered lists.
 *
 * ABSTRACTION  — abstract class with abstract methods getId() and search().
 *                Subclasses define HOW to get an ID from their specific entity.
 *
 * OPTIONAL<T>  — findById returns Optional to safely handle "not found" without null.
 *
 * NOTE: This class implements Searchable<T> — a generic interface used on a generic class.
 */
public abstract class Repository<T> implements Searchable<T> {

    // Collections Framework: HashMap — key = entity ID, value = the entity
    // O(1) average time for add, remove, and lookup
    protected final Map<String, T> store = new HashMap<>();

    /** Add an entity — ID is extracted by subclass via getId() */
    public void add(T item) {
        store.put(getId(item), item);
    }

    /** Remove by ID. Returns true if something was actually removed. */
    public boolean remove(String id) {
        return store.remove(id) != null;
    }

    /**
     * Find by ID — returns Optional<T>, not null.
     * Optional forces the caller to handle the "not found" case explicitly.
     *
     *   Optional.ofNullable(x):
     *     - if x is not null → Optional containing x
     *     - if x is null     → Optional.empty()
     */
    public Optional<T> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Return all entities as an ArrayList (a snapshot — not the live map) */
    public List<T> getAll() {
        return new ArrayList<>(store.values());
    }

    /** Check if an ID already exists */
    public boolean exists(String id) {
        return store.containsKey(id);
    }

    /** Total count of stored entities */
    public int count() {
        return store.size();
    }

    /** Wipe all data (used on app close or test reset) */
    public void clear() {
        store.clear();
    }

    // -------------------------------------------------------------------
    // Abstract Methods — subclasses MUST define these
    // -------------------------------------------------------------------

    /**
     * Extract the unique ID string from an entity.
     * e.g. for Room → return room.getRoomId()
     *      for Guest → return guest.getGuestId()
     */
    protected abstract String getId(T item);

    /**
     * Search entities matching a query string.
     * Each subclass defines what fields to search on.
     * From Searchable<T> interface.
     */
    @Override
    public abstract List<T> search(String query);
}
