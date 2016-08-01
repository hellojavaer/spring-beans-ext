package org.hellojavaer.spring.beans.ext.config.text.service.impl;

import org.hellojavaer.spring.beans.ext.config.text.service.TestService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:hellojavaer@gmail.com">zoukaiming</a>
 */
@Service
public class TestServiceImpl implements TestService, InitializingBean, DisposableBean {

    @Value("${env}")
    private String env;

    public void afterPropertiesSet() throws Exception {
        System.out.println("======>" + env);
    }

    public void destroy() throws Exception {
        System.out.println("destroy");
    }
}
