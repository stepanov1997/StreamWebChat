package com.swc.it;

import com.swc.runner.KubernetesDeployment;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ITLauncherTest {
    /**
     * @return Names of components.
     */
    Class<? extends KubernetesDeployment>[] components();
}
