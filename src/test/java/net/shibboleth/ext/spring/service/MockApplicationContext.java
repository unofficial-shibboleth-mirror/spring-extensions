/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.ext.spring.service;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;

/**
 * Placeholder, which can be set on test {@link AbstractServiceableComponent}s to stop the
 * "must be null" test firing.
 */
public class MockApplicationContext implements ConfigurableApplicationContext{

    /** {@inheritDoc} */
    public String getId() {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public String getApplicationName() {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public String getDisplayName() {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public long getStartupDate() {
        fail();
        return 0;
    }

    /** {@inheritDoc} */
    public ApplicationContext getParent() {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public boolean containsBeanDefinition(String beanName) {
        fail();
        return false;
    }

    /** {@inheritDoc} */
    public int getBeanDefinitionCount() {
        fail();
        return 0;
    }

    /** {@inheritDoc} */
    public String[] getBeanDefinitionNames() {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public String[] getBeanNamesForType(ResolvableType type) {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public String[] getBeanNamesForType(Class<?> type) {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
            throws BeansException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
            throws BeansException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
            throws NoSuchBeanDefinitionException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public Object getBean(String name) throws BeansException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public Object getBean(String name, Object... args) throws BeansException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public boolean containsBean(String name) {
        fail();
        return false;
    }

    /** {@inheritDoc} */
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        fail();
        return false;
    }

    /** {@inheritDoc} */
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        fail();
        return false;
    }

    /** {@inheritDoc} */
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        fail();
        return false;
    }

    /** {@inheritDoc} */
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        fail();
        return false;
    }

    /** {@inheritDoc} */
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public String[] getAliases(String name) {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public BeanFactory getParentBeanFactory() {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public boolean containsLocalBean(String name) {
        fail();
        return false;
    }

    /** {@inheritDoc} */
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public void publishEvent(Object event) {
        fail();        
    }

    /** {@inheritDoc} */
    public Resource[] getResources(String locationPattern) throws IOException {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public Resource getResource(String location) {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public ClassLoader getClassLoader() {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public void start() {
        fail();
    }

    /** {@inheritDoc} */
    public void stop() {
        fail();
    }

    /** {@inheritDoc} */
    public boolean isRunning() {
        fail();
        return false;
    }

    /** {@inheritDoc} */
    public void setId(String id) {
        fail();
    }

    /** {@inheritDoc} */
    public void setParent(ApplicationContext parent) {
        fail();
    }

    /** {@inheritDoc} */
    public void setEnvironment(ConfigurableEnvironment environment) {
        fail();
    }

    /** {@inheritDoc} */
    public ConfigurableEnvironment getEnvironment() {
        fail();
        return null;
    }

    /** {@inheritDoc} */
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
        fail();
    }

    /** {@inheritDoc} */
    public void addApplicationListener(ApplicationListener<?> listener) {
        fail();
    }

    /** {@inheritDoc} */
    public void addProtocolResolver(ProtocolResolver resolver) {
        fail();
    }

    /** {@inheritDoc} */
    public void refresh() throws BeansException, IllegalStateException {
        fail();
    }

    /** {@inheritDoc} */
    public void registerShutdownHook() {
        fail();
    }

    /** {@inheritDoc} */
    public void close() {
    }

    /** {@inheritDoc} */
    public boolean isActive() {
        fail();
        return false;
    }

    /** {@inheritDoc} */
    public ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException {
        fail();
        return null;
    }
}
