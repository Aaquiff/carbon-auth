/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.auth.user.info.internal;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.auth.scim.SCIMManager;
import org.wso2.carbon.auth.scim.exception.AuthUserManagementException;
import org.wso2.carbon.auth.user.info.configuration.UserInfoConfigurationService;
import org.wso2.carbon.auth.user.info.configuration.models.UserInfoConfiguration;
import org.wso2.carbon.auth.user.info.util.UserInfoUtil;
import org.wso2.carbon.auth.user.store.configuration.UserStoreConfigurationService;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;

/**
 * Class used to activate configuration loading
 */
@Component(
        name = "org.wso2.carbon.auth.user.info",
        immediate = true
)
public class ConfigurationActivator {

    private static final Logger log = LoggerFactory.getLogger(ServiceReferenceHolder.class);
    private ServiceRegistration registration;

    /**
     * Get the ConfigProvider service.
     * This is the bind method that gets called for ConfigProvider service registration that satisfy the policy.
     *
     * @param configProvider the ConfigProvider service that is registered as a service.
     */
    @Reference(
            name = "carbon.config.provider",
            service = ConfigProvider.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterConfigProvider")
    protected void registerConfigProvider(ConfigProvider configProvider) {

        ServiceReferenceHolder.getInstance().setConfigProvider(configProvider);

    }

    /**
     * This is the unbind method for the above reference that gets called for ConfigProvider instance un-registrations.
     *
     * @param configProvider the ConfigProvider service that get unregistered.
     */
    protected void unregisterConfigProvider(ConfigProvider configProvider) {

        ServiceReferenceHolder.getInstance().setConfigProvider(null);
    }

    @Activate
    protected void activate(ComponentContext componentContext) throws ConfigurationException {

        ConfigProvider configProvider = ServiceReferenceHolder.getInstance().getConfigProvider();
        UserInfoConfiguration config = configProvider.getConfigurationObject(UserInfoConfiguration.class);
        UserInfoConfigurationService userInfoConfigurationService = new UserInfoConfigurationService(config);
        ServiceReferenceHolder.getInstance().setUserInfoConfigurationService(userInfoConfigurationService);
        registration = componentContext.getBundleContext().registerService(
                UserInfoConfigurationService.class.getName(),
                userInfoConfigurationService, null);
        try {
            UserInfoUtil.setUserManager(SCIMManager.getInstance().getCarbonAuthSCIMUserManager());
        } catch (AuthUserManagementException e) {
            String errorMsg = "Error while retrieving SCIM Manager instance.";
            log.error(errorMsg, e);
        }
    }

    @Reference(
            name = "org.wso2.carbon.auth.user.store",
            service = UserStoreConfigurationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterUserStoreConfigurationService"
    )
    protected void registerUserStoreConfigurationService(UserStoreConfigurationService service) {

        ServiceReferenceHolder.getInstance().setUserStoreConfigurationService(service);

        if (log.isDebugEnabled()) {
            log.debug("User store configuration service registered successfully.");
        }
    }

    protected void unregisterUserStoreConfigurationService(UserStoreConfigurationService service) {

        ServiceReferenceHolder.getInstance().setUserStoreConfigurationService(null);

        if (log.isDebugEnabled()) {
            log.debug("User store configuration service unregistered.");
        }
    }

    @Deactivate
    protected void deactivate() {

        if (registration != null) {
            registration.unregister();
        }

    }
}
