/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hellojavaer.spring.beans.ext.config;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * @author <a href="mailto:hellojavaer@gmail.com">zoukaiming</a>
 */
public class PropertyPlaceholderConfigurer extends org.springframework.beans.factory.config.PropertyPlaceholderConfigurer {

    /** Logger available to subclasses */
    protected final Log        logger                   = LogFactory.getLog(getClass());

    private ApplicationContext applicationContext;

    private boolean            resolvePlaceholderAtOnce = false;

    private static int         sequenceCount            = Integer.MIN_VALUE;
    private int                sequence;

    private Resource[]         locations;

    public PropertyPlaceholderConfigurer() {
        sequence = sequenceCount++;
    }

    @Override
    public void setLocations(Resource... locations) {
        this.locations = locations;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setResolvePlaceholderAtOnce(boolean resolvePlaceholderAtOnce) {
        this.resolvePlaceholderAtOnce = resolvePlaceholderAtOnce;
    }

    protected Resource[] getLocatons() throws IOException {
        return this.locations;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (resolvePlaceholderAtOnce) {
            super.postProcessBeanFactory(beanFactory);
        } else {
            Map<String, PropertyPlaceholderConfigurer> map = this.applicationContext.getBeansOfType(PropertyPlaceholderConfigurer.class);
            if (map != null && !map.isEmpty()) {
                int minSequence = Integer.MIN_VALUE;
                boolean first = true;
                for (Map.Entry<String, PropertyPlaceholderConfigurer> entry : map.entrySet()) {
                    PropertyPlaceholderConfigurer config = entry.getValue();
                    if (first) {
                        first = false;
                        minSequence = config.sequence;
                    } else {
                        if (config.sequence < minSequence) {
                            minSequence = config.sequence;
                        }
                        if (minSequence < sequence) {
                            break;
                        }
                    }
                }
                if (minSequence == sequence) {
                    List<Resource> totalResources = new LinkedList<Resource>();
                    for (Map.Entry<String, PropertyPlaceholderConfigurer> entry : map.entrySet()) {
                        PropertyPlaceholderConfigurer config = entry.getValue();
                        try {
                            Resource[] resources = config.getLocatons();
                            if (resources != null) {
                                for (Resource r : resources) {
                                    totalResources.add(r);
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("", e);
                        }
                    }
                    for (Resource r : totalResources) {
                        logger.info(r);
                    }
                    super.setLocations(totalResources.toArray(new Resource[totalResources.size()]));
                    super.postProcessBeanFactory(beanFactory);
                } else {
                    // do nothing
                }
            }
        }
    }
}
