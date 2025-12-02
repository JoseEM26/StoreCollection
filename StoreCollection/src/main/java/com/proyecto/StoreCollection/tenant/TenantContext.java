// src/main/java/com/proyecto/StoreCollection/tenant/TenantContext.java
package com.proyecto.StoreCollection.tenant;

public class TenantContext {
    private static final ThreadLocal<Integer> currentTenantId = new ThreadLocal<>();

    public static void setTenantId(Integer tenantId) {
        currentTenantId.set(tenantId);
    }

    public static Integer getTenantId() {
        return currentTenantId.get();
    }

    public static void clear() {
        currentTenantId.remove();
    }

    public static boolean isSet() {
        return currentTenantId.get() != null;
    }
}