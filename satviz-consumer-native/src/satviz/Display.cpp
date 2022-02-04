#include <satviz/Display.hpp>

#include <glad/gl.h>

namespace satviz {
namespace video {

sf::ContextSettings Display::makeContextSettings() {
  sf::ContextSettings settings;
  settings.attributeFlags |= sf::ContextSettings::Core;
  settings.attributeFlags |= sf::ContextSettings::Debug;
  settings.majorVersion = 3;
  settings.minorVersion = 3;
  return settings;
}

void Display::loadGlExtensions() {
  gladLoaderLoadGL();
  glGenBuffers(NUM_PBOS, pbos);
  onResize();
}

void Display::onResize() {
  glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[PBO_IN_PROGRESS]);
  glBufferData(GL_PIXEL_PACK_BUFFER, 4 * width * height, NULL, GL_STREAM_READ);
  glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[PBO_READY]);
  glBufferData(GL_PIXEL_PACK_BUFFER, 4 * width * height, NULL, GL_STREAM_READ);
  glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
}

Display::~Display() {
  glDeleteBuffers(NUM_PBOS, pbos);
}

void Display::startFrame() {
  glViewport(0, 0, width, height);
  glClearColor(0.3f, 0.3f, 0.3f, 0.0f);
  glClear(GL_COLOR_BUFFER_BIT);
}

void Display::endFrame() {
  // Switch PBOs
  {
    unsigned temp = pbos[PBO_READY];
    pbos[PBO_READY] = pbos[PBO_IN_PROGRESS];
    pbos[PBO_IN_PROGRESS] = temp;
  }
  displayFrame();
}

void Display::transferFrame() {
  glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[PBO_IN_PROGRESS]);
  glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, (void *) 0);
  glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
}

VideoFrame Display::grabFrame() {
  glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[PBO_READY]);
  void *pixels = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY);
  VideoFrame frame = VideoFrame::fromBgraImage(width, height, pixels);
  glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
  glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
  return frame;
}

} // namespace video
} // namespace satviz
