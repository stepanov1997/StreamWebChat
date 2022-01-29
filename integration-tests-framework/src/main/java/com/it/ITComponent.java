package com.it;

import java.lang.annotation.*;

/**
 * Indicates that an annotated class is a IT Component, which means that it corresponds to a kubernetes
 * deployment and that it should be present only on classes which implement {@link KubernetesDeployment}.
 * <p>
 * It provides additional information about a component, like name of kubernetes deployment and components it depends
 * on, which allows IT to manage its lifecycle.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ITComponent {

    /**
     * Name of the component. It must match name of the corresponding kubernetes deployment.
     *
     * @return name of the component
     */
    String value();

    /**
     * List of components that must be started before this one.
     *
     * @return List of components ir depends on
     */
    Class<? extends KubernetesDeployment>[] dependsOn() default {};
}
