package com.ccems.model;

import com.ccems.exception.BusinessException;

public enum ChatScope {
    EVENT, CLUB;

    /** Parses the URL form ("event" or "club"). */
    public static ChatScope parse(String text) {
        if (text != null) {
            for (ChatScope s : values()) {
                if (s.name().equalsIgnoreCase(text.trim())) return s;
            }
        }
        throw BusinessException.badRequest("Unknown chat type. Use event or club.");
    }
}
