plugins {
    id("base-conventions")
}

dependencies {
    implementation(projects.api.shops)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.interfaces.bank)
}
