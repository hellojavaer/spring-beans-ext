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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * @author <a href="mailto:hellojavaer@gmail.com">zoukaiming</a>
 */
public class MultipleEnvPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements ApplicationContextAware {

    /** Logger available to subclasses */
    protected final Log         logger                   = LogFactory.getLog(getClass());

    private static final String ENV_KEY                  = "env";
    private static final String DEFAULT_ENV_MAPPING      = "{*}->{0}";
    private ApplicationContext  applicationContext;

    private String              rule                     = DEFAULT_ENV_MAPPING;
    private String              env;
    private String              baseLocation;
    private boolean             resolvePlaceholderAtOnce = false;

    private static int          sequenceCount            = Integer.MIN_VALUE;
    private int                 sequence;

    public MultipleEnvPropertyPlaceholderConfigurer() {
        sequence = sequenceCount++;
    }

    @Override
    public void setLocations(Resource... locations) {
        throw new IllegalArgumentException("can't set value for this parameter");
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setResolvePlaceholderAtOnce(boolean resolvePlaceholderAtOnce) {
        this.resolvePlaceholderAtOnce = resolvePlaceholderAtOnce;
    }

    public void setBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
    }

    /**
     * {*}->{0};dev*,->dev;*->cn
     * @param rule
     */
    public void setRule(String rule) {
        this.rule = rule;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    protected List<Resource> getLocatons() throws IOException {
        List<Resource> resources = new ArrayList<Resource>();
        if (env == null) {
            env = applicationContext.getEnvironment().getProperty(ENV_KEY);
            if (env == null) {
                env = "";
            }
        }
        env = env.trim();
        String path = applicationContext.getEnvironment().resolveRequiredPlaceholders(baseLocation);
        List<File> basePaths = getFileDirs(path);
        if (basePaths.isEmpty()) {
            throw new IllegalArgumentException("no basePath found for:" + baseLocation);
        } else {
            logger.info(basePaths);
        }
        for (File baseFilePath : basePaths) {
            resources.addAll(procOneBasePath(env, baseFilePath));
        }
        return resources;
    }

    private List<Resource> procOneBasePath(String env, File baseFilePath) {
        List<Resource> resources = new ArrayList<Resource>();
        if (!baseFilePath.exists() || !baseFilePath.isDirectory()) {
            throw new IllegalArgumentException("");
        }
        String[] envMappings = rule.split(";");
        Map<String, File> fileQueryCache = new HashMap<String, File>();
        recuBuildQueryCacheForDir(baseFilePath, null, fileQueryCache);

        boolean matched = false;
        for (String envMapping : envMappings) {
            envMapping = envMapping.trim();
            if (envMapping.equals("")) {
                continue;
            }
            String[] envKv = envMapping.split("->");
            if (envKv.length > 2) {
                throw new IllegalArgumentException(envMapping);
            }
            String key = envKv.length > 0 ? envKv[0].trim() : "";
            String value = envKv.length > 1 ? envKv[1].trim() : "";

            String[] subKeys = key.split(",");

            List<String> list = new ArrayList<String>();
            list.add(value);
            if (subKeys == null || subKeys.length == 0) {
                subKeys = new String[] { "" };
            }
            for (String k : subKeys) {
                if (PatternMatchUtils.simpleMatch(k, env, list)) {
                    matched = true;
                    String convertedValue = list.get(0);
                    logger.info(String.format("env:%s, envMapping:%s, basePath:%s, [%s->%s] map success.", env,
                                              this.rule, this.baseLocation, k, value));
                    if ("".equals(convertedValue)) {//
                        loadFiles(baseFilePath, false, resources);
                    } else {
                        File file = fileQueryCache.get(convertedValue);
                        if (file == null) {
                            throw new IllegalArgumentException("no env:" + convertedValue + " found under "
                                                               + baseFilePath.getPath());
                        } else {
                            loadFiles(file, true, resources);
                        }
                    }
                    break;
                }
            }
            if (matched) {
                break;
            }
        }
        if (!matched) {
            throw new IllegalArgumentException(String.format("env:%s, envMapping:%s, can't find matched mapping.", env,
                                                             rule));
        }
        return resources;
    }

    private void recuBuildQueryCacheForDir(File basePath, String pathPrefix, Map<String, File> queryCache) {
        File[] files = basePath.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String curPath = file.getName();
                    if (pathPrefix != null) {
                        curPath = pathPrefix + "/" + file.getName();
                    }
                    queryCache.put(curPath, file);
                    recuBuildQueryCacheForDir(file, curPath, queryCache);
                }
            }
        }
    }

    private List<File> getFileDirs(String basePath) throws IOException {
        List<File> dirs = new ArrayList<File>();
        Resource[] resources = applicationContext.getResources(basePath);
        if (resources != null) {
            for (Resource resource : resources) {
                if (resource != null && resource.getFile() != null && resource.getFile().isDirectory()) {
                    dirs.add(resource.getFile());
                }
            }
        }
        return dirs;
    }

    private void loadFiles(File path, boolean recu, List<Resource> resources) {
        File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    FileSystemResource fsr = new FileSystemResource(file);
                    resources.add(fsr);
                } else if (file.isDirectory() && recu) {
                    loadFiles(file, recu, resources);
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (resolvePlaceholderAtOnce) {
            super.postProcessBeanFactory(beanFactory);
        } else {
            Map<String, MultipleEnvPropertyPlaceholderConfigurer> map = this.applicationContext.getBeansOfType(MultipleEnvPropertyPlaceholderConfigurer.class);
            if (map != null && !map.isEmpty()) {
                int minSequence = Integer.MIN_VALUE;
                boolean first = true;
                for (Map.Entry<String, MultipleEnvPropertyPlaceholderConfigurer> entry : map.entrySet()) {
                    MultipleEnvPropertyPlaceholderConfigurer config = entry.getValue();
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
                    for (Map.Entry<String, MultipleEnvPropertyPlaceholderConfigurer> entry : map.entrySet()) {
                        MultipleEnvPropertyPlaceholderConfigurer config = entry.getValue();
                        try {
                            List<Resource> resources = config.getLocatons();
                            totalResources.addAll(resources);
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
