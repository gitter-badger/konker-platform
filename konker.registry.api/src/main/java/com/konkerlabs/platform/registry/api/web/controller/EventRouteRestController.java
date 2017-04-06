package com.konkerlabs.platform.registry.api.web.controller;

import com.konkerlabs.platform.registry.api.exceptions.BadServiceResponseException;
import com.konkerlabs.platform.registry.api.exceptions.NotFoundResponseException;
import com.konkerlabs.platform.registry.api.model.*;
import com.konkerlabs.platform.registry.business.model.*;
import com.konkerlabs.platform.registry.business.model.EventRoute.RouteActor;
import com.konkerlabs.platform.registry.business.services.api.*;
import com.konkerlabs.platform.registry.business.services.api.EventRouteService.Validations;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@Scope("request")
@RequestMapping(value = "/routes")
@Api(tags = "routes")
public class EventRouteRestController implements InitializingBean {

    @Autowired
    private EventRouteService eventRouteService;

    @Autowired
    private DeviceRegisterService deviceRegisterService;

    @Autowired
    private TransformationService transformationService;

    @Autowired
    private RestDestinationService restDestinationService;

    @Autowired
    private User user;

    private Set<String> validationsCode = new HashSet<>();

    @GetMapping(path = "/")
    @PreAuthorize("hasAuthority('LIST_ROUTES')")
    @ApiOperation(
            value = "List all routes by organization",
            response = EventRouteVO.class)
    public List<EventRouteVO> list() throws BadServiceResponseException {

        Tenant tenant = user.getTenant();

        ServiceResponse<List<EventRoute>> routeResponse = eventRouteService.getAll(tenant);

        if (!routeResponse.isOk()) {
            throw new BadServiceResponseException(user, routeResponse, validationsCode);
        } else {
            return new EventRouteVO().apply(routeResponse.getResult());
        }

    }

    @GetMapping(path = "/{routeGuid}")
    @PreAuthorize("hasAuthority('SHOW_DEVICE_ROUTE')")
    @ApiOperation(
            value = "Get a route by guid",
            response = RestResponse.class
    )
    public EventRouteVO read(@PathVariable("routeGuid") String routeGuid) throws BadServiceResponseException, NotFoundResponseException {

        Tenant tenant = user.getTenant();

        ServiceResponse<EventRoute> routeResponse = eventRouteService.getByGUID(tenant, routeGuid);

        if (!routeResponse.isOk()) {
            throw new NotFoundResponseException(user, routeResponse);
        } else {
            return new EventRouteVO().apply(routeResponse.getResult());
        }

    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_DEVICE_ROUTE')")
    @ApiOperation(value = "Create a route")
    public EventRouteVO create(
            @ApiParam(name = "body", required = true)
            @RequestBody EventRouteInputVO routeForm) throws BadServiceResponseException {

        Tenant tenant = user.getTenant();

        RouteActor incoming = getRouteActor(tenant, routeForm.getIncoming());
        RouteActor outgoing = getRouteActor(tenant, routeForm.getOutgoing());
        Transformation transformation = getTransformation(tenant, routeForm);

        EventRoute route = EventRoute.builder()
                .name(routeForm.getName())
                .description(routeForm.getDescription())
                .incoming(incoming)
                .outgoing(outgoing)
                .filteringExpression(routeForm.getFilteringExpression())
                .transformation(transformation)
                .active(true)
                .build();

        ServiceResponse<EventRoute> routeResponse = eventRouteService.save(tenant, route);

        if (!routeResponse.isOk()) {
            throw new BadServiceResponseException(user, routeResponse, validationsCode);
        } else {
            return new EventRouteVO().apply(routeResponse.getResult());
        }

    }

    private Transformation getTransformation(Tenant tenant, EventRouteInputVO routeForm) throws BadServiceResponseException {

        String guid = routeForm.getTransformationGuid();

        if (StringUtils.isNoneBlank(guid)) {
            ServiceResponse<Transformation> transformationResponse = transformationService.get(tenant, null, guid);
            if (transformationResponse.isOk()) {
                Transformation transformation = transformationResponse.getResult();
                return transformation;
            } else {
                throw new BadServiceResponseException(user, transformationResponse, validationsCode);
            }
        }

        return null;

    }

    @SuppressWarnings("serial")
    private RouteActor getRouteActor(Tenant tenant, RouteActorVO routeForm) throws BadServiceResponseException {

        RouteActor routeActor = RouteActor.builder().build();

        if (routeForm == null) {
            return null;
        }

        if (RouteActorType.DEVICE.name().equalsIgnoreCase(routeForm.getType())) {
            ServiceResponse<Device> deviceResponse = deviceRegisterService.getByDeviceGuid(tenant, null, routeForm.getGuid());
            if (deviceResponse.isOk()) {
                routeActor.setDisplayName(deviceResponse.getResult().getName());
                routeActor.setUri(deviceResponse.getResult().toURI());
                routeActor.setData(new HashMap<String, String>() {{ put(EventRoute.DEVICE_MQTT_CHANNEL, routeForm.getChannel()); }} );
                return routeActor;
            } else {
                throw new BadServiceResponseException(user, deviceResponse, validationsCode);
            }
        } else if (RouteActorType.REST.name().equalsIgnoreCase(routeForm.getType())) {
            ServiceResponse<RestDestination> restResponse = restDestinationService.getByGUID(tenant, routeForm.getGuid());
            if (restResponse.isOk()) {
                routeActor.setDisplayName(restResponse.getResult().getName());
                routeActor.setUri(restResponse.getResult().toURI());
                routeActor.setData(new HashMap<String, String>() {} );
                return routeActor;
            } else {
                throw new BadServiceResponseException(user, restResponse, validationsCode);
            }
        }

        return null;

    }


    @PutMapping(path = "/{routeGuid}")
    @PreAuthorize("hasAuthority('EDIT_DEVICE_ROUTE')")
    @ApiOperation(value = "Update a route")
    public void update(
            @PathVariable("routeGuid") String routeGuid,
            @ApiParam(name = "body", required = true)
            @RequestBody EventRouteInputVO routeForm) throws BadServiceResponseException {

        Tenant tenant = user.getTenant();

        EventRoute routeFromDB = null;
        ServiceResponse<EventRoute> routeResponse = eventRouteService.getByGUID(tenant, routeGuid);

        if (!routeResponse.isOk()) {
            throw new BadServiceResponseException(user, routeResponse, validationsCode);
        } else {
            routeFromDB = routeResponse.getResult();
        }

        RouteActor incoming = getRouteActor(tenant, routeForm.getIncoming());
        RouteActor outgoing = getRouteActor(tenant, routeForm.getOutgoing());
        Transformation transformation = getTransformation(tenant, routeForm);

        // update fields
        routeFromDB.setName(routeForm.getName());
        routeFromDB.setDescription(routeForm.getDescription());
        routeFromDB.setIncoming(incoming);
        routeFromDB.setOutgoing(outgoing);
        routeFromDB.setTransformation(transformation);
        routeFromDB.setFilteringExpression(routeForm.getFilteringExpression());
        routeFromDB.setActive(routeForm.isActive());

        ServiceResponse<EventRoute> updateResponse = eventRouteService.update(tenant, routeGuid, routeFromDB);

        if (!updateResponse.isOk()) {
            throw new BadServiceResponseException(user, updateResponse, validationsCode);
        }

    }

    @DeleteMapping(path = "/{routeGuid}")
    @PreAuthorize("hasAuthority('REMOVE_DEVICE_ROUTE')")
    @ApiOperation(value = "Delete a route")
    public void delete(@PathVariable("routeGuid") String routeGuid) throws BadServiceResponseException, NotFoundResponseException {

        Tenant tenant = user.getTenant();

        ServiceResponse<EventRoute> routeResponse = eventRouteService.remove(tenant, routeGuid);

        if (!routeResponse.isOk()) {
            if (routeResponse.getResponseMessages().containsKey(Validations.EVENT_ROUTE_NOT_FOUND.getCode())) {
                throw new NotFoundResponseException(user, routeResponse);
            } else {
                throw new BadServiceResponseException(user, routeResponse, validationsCode);
            }
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {

        for (Validations value : EventRouteService.Validations.values()) {
            validationsCode.add(value.getCode());
        }

        for (com.konkerlabs.platform.registry.business.model.EventRoute.Validations value : EventRoute.Validations.values()) {
            validationsCode.add(value.getCode());
        }

        for (com.konkerlabs.platform.registry.business.model.Transformation.Validations value : Transformation.Validations.values()) {
            validationsCode.add(value.getCode());
        }

        for (com.konkerlabs.platform.registry.business.services.api.TransformationService.Validations value : TransformationService.Validations.values()) {
            validationsCode.add(value.getCode());
        }

    }

}
