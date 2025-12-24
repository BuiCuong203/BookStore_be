package com.vn.backend.util.enums;

public enum RoleEnum {
    ROLE_ADMIN,
    ROLE_SELLER,
    ROLE_USER;

    public static RoleEnum fromDb(String roleName) {
        if (roleName == null) return null;

        roleName = roleName.trim().toUpperCase();

        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        try {
            return RoleEnum.valueOf(roleName);
        } catch (Exception e) {
            return null;
        }
    }
}
