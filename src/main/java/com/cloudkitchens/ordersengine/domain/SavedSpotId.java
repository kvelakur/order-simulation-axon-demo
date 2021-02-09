package com.cloudkitchens.ordersengine.domain;

import java.util.UUID;

public class SavedSpotId extends GlobalId {
    private static final long serialVersionUID = -7782233912241300174L;

    public SavedSpotId(final String value) {
        super(value);
    }

    public SavedSpotId() {
        super(UUID.randomUUID().toString());
    }
}
