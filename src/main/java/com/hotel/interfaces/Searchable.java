package com.hotel.interfaces;

import java.util.List;

/**
 * Generic interface for search functionality.
 *
 * KEY CONCEPT: Generics + Interface combined.
 * The <T> is a "type parameter" — a placeholder for a real type.
 * When a class implements Searchable<Room>, T becomes Room.
 * When a class implements Searchable<Guest>, T becomes Guest.
 *
 * This means ONE interface works for ALL entity types —
 * no need to write RoomSearchable, GuestSearchable, etc. separately.
 *
 * OOP PRINCIPLE: Abstraction — every repository promises to support search,
 * but each defines its own search logic.
 */
public interface Searchable<T> {

    /**
     * Search entities matching the given query string.
     *
     * @param query the search keyword (name, ID, phone, etc.)
     * @return a List of matching entities of type T
     */
    List<T> search(String query);
}
