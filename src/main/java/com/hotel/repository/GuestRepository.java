package com.hotel.repository;

import com.hotel.model.Guest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for Guest entities.
 *
 * GENERICS: Extends Repository<Guest> — T is fixed as Guest.
 * COLLECTIONS: Inherits HashMap<String, Guest> from parent for O(1) ID lookup.
 *
 * Simpler than RoomRepository — no extra data structure needed.
 * Search covers name, ID, phone, and email.
 */
public class GuestRepository extends Repository<Guest> {

    /** Tells the parent how to extract the unique key from a Guest */
    @Override
    protected String getId(Guest guest) {
        return guest.getGuestId();
    }

    /**
     * Search across name, guestId, phone, and email.
     * One query string checks all four fields — broadest possible search.
     */
    @Override
    public List<Guest> search(String query) {
        String lq = query.toLowerCase();
        return store.values().stream()
                .filter(g -> g.getName().toLowerCase().contains(lq)
                        || g.getGuestId().toLowerCase().contains(lq)
                        || g.getPhone().contains(lq)
                        || g.getEmail().toLowerCase().contains(lq))
                .collect(Collectors.toList());
    }

    /**
     * Find a guest by exact phone number.
     * Returns Optional — phone might not match any guest.
     * Uses Stream.findFirst() to return the first match.
     */
    public Optional<Guest> findByPhone(String phone) {
        return store.values().stream()
                .filter(g -> g.getPhone().equals(phone))
                .findFirst();
    }
}
