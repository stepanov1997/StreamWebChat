package com.it;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ITTest {
    /** @return Names of components. */
    Class<? extends KubernetesDeployment>[] components();
}
