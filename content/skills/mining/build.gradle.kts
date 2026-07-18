plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.areaChecker)
    implementation(projects.api.attr)
    implementation(projects.api.pluginCommons)
}
