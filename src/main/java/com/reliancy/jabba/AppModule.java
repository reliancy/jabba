package com.reliancy.jabba;
/** Special controller interface which is implemented by modules.
 * Modules are classes that publish or retract middleware or endpoints into application.
 * They allow us to compose an application of disparate apis.
 */
public interface AppModule {
    void publish(App app);
    default void retract(App app){};
}
