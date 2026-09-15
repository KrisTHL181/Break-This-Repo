#include "AppPlatform_glfw.h"

float AppPlatform_glfw::getPixelsPerMillimeter() {
    GLFWmonitor* monitor = glfwGetPrimaryMonitor();

    int width_mm, height_mm;
    glfwGetMonitorPhysicalSize(monitor, &width_mm, &height_mm);

    const GLFWvidmode* mode = glfwGetVideoMode(monitor);

    return (float)mode->width / (float)width_mm;
}