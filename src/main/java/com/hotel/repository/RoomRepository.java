package com.hotel.repository;

import com.hotel.model.Room;
import com.hotel.model.enums.RoomStatus;
import com.hotel.model.enums.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Repository for Room entities.
 *
 * KEY CONCEPTS:
 *
 * COLLECTIONS FRAMEWORK:
 *   - Inherits HashMap<String, Room> from Repository<Room> for O(1) lookups.
 *   - Adds TreeMap<String, Room> for SORTED room listings (sorted by roomId).
 *   - Uses Stream API + Collectors for filtering rooms by status/type.
 *
 * GENERICS:
 *   - Extends Repository<Room> — T is now fixed as Room.
 *   - Provides concrete getId() and search() for Room-specific fields.
 *
 * TreeMap vs HashMap:
 *   HashMap  → unordered, O(1) average for get/put
 *   TreeMap  → always sorted by key, O(log n) for get/put
 *   We use BOTH — HashMap (inherited) for fast lookup, TreeMap for display order.
 */
public class RoomRepository extends Repository<Room> {

    // TreeMap keeps rooms sorted alphabetically by roomId at all times
    private final TreeMap<String, Room> sortedRooms = new TreeMap<>();

    /** Override add() to keep BOTH the HashMap and TreeMap in sync */
    @Override
    public void add(Room room) {
        super.add(room);                          // adds to HashMap (O(1))
        sortedRooms.put(room.getRoomId(), room);  // adds to TreeMap (O(log n))
    }

    /** Override remove() to keep both in sync */
    @Override
    public boolean remove(String id) {
        sortedRooms.remove(id);
        return super.remove(id);
    }

    /** Tells the parent Repository how to get the ID from a Room */
    @Override
    protected String getId(Room room) {
        return room.getRoomId();
    }

    /**
     * SEARCH — filters rooms by ID, type name, or description.
     * Uses Stream API (filter + collect) — modern Java style.
     */
    @Override
    public List<Room> search(String query) {
        String lq = query.toLowerCase();
        return store.values().stream()
                .filter(r -> r.getRoomId().toLowerCase().contains(lq)
                        || r.getType().getDisplayName().toLowerCase().contains(lq)
                        || r.getDescription().toLowerCase().contains(lq))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------
    // Room-specific queries — using Streams for filtering
    // -------------------------------------------------------------------

    /** All available rooms — shown in booking form dropdown */
    public List<Room> getAvailableRooms() {
        return store.values().stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    /** Filter by room type (Standard / Deluxe / Suite) */
    public List<Room> getByType(RoomType type) {
        return store.values().stream()
                .filter(r -> r.getType() == type)
                .collect(Collectors.toList());
    }

    /** Filter by any status */
    public List<Room> getByStatus(RoomStatus status) {
        return store.values().stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Returns rooms sorted by ID using TreeMap order.
     * TreeMap always maintains ascending key order — no extra sorting needed.
     */
    public List<Room> getAllSorted() {
        return new ArrayList<>(sortedRooms.values());
    }

    /** Count rooms of a specific status — used in Dashboard stats */
    public long countByStatus(RoomStatus status) {
        return store.values().stream()
                .filter(r -> r.getStatus() == status)
                .count();
    }
}
