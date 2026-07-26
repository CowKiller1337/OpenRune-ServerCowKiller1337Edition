plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.orCache)
    implementation(projects.api.attr)
    implementation(projects.api.instances)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.skills.utils)
}
