package com.konkerlabs.platform.registry.business.services.api;

import com.konkerlabs.platform.registry.business.exceptions.BusinessException;
import com.konkerlabs.platform.registry.business.model.Device;
import com.konkerlabs.platform.registry.business.model.Event;
import com.konkerlabs.platform.registry.business.model.Tenant;

import java.util.List;

public interface DeviceRegisterService {
    ServiceResponse<Device> register(Tenant tenant, Device device);
    ServiceResponse<Device> update(String id, Device device);
    List<Device> getAll(Tenant tenant);
    Device findById(String id);
    Device findByApiKey(String apiKey);
    Device findByTenantDomainNameAndDeviceId(String tenantDomainName, String deviceId);
    ServiceResponse<Device> switchActivation(String id);
}