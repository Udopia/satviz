#ifndef SATVIZ_THEORA_ENCODER_HPP_
#define SATVIZ_THEORA_ENCODER_HPP_

#include <satviz/VideoEncoder.hpp>

namespace satviz {
namespace video {

struct TheoraStream;

/**
 *
 */
class TheoraEncoder : VideoEncoder {
private:
  TheoraStream *stream;

public:
  TheoraEncoder();
  ~TheoraEncoder();
  bool startRecording(const char *filename, int width, int height);
  void submitFrame(VideoFrame &frame, bool last);
};

} // namespace video
} // namespace satviz

#endif
