#include <vector>

#include <satviz/Graph.hpp>
#include <satviz/Display.hpp>
#include <satviz/OnscreenDisplay.hpp>
#include <satviz/OffscreenDisplay.hpp>
#include <satviz/VideoController.hpp>
#include <satviz/VideoEncoder.hpp>
#include <satviz/TheoraEncoder.hpp>

using namespace satviz::graph;

extern "C" {

#include <satviz/Bindings.h>

void *satviz_new_graph(size_t nodes) {
  return new Graph { nodes };
}

void satviz_release_graph(void *graph) {
  delete (Graph*) graph;
}

void satviz_recalculate_layout(void *graph) {
  reinterpret_cast<Graph*>(graph)->recalculateLayout();
}

void satviz_adapt_layout(void *graph) {
  // TODO not implemented yet
  (void) graph;
  //reinterpret_cast<Graph*>(graph)->adaptLayout();
}

char *satviz_serialize(void *graph) {
  // TODO not implemented yet
  (void) graph;
  return nullptr;
  //return reinterpret_cast<Graph*>(graph)->serialize().str().c_str();
}

void satviz_deserialize(void *graph, const char *str) {
  std::stringbuf buf { std::string { str } };
  // TODO not implemented yet
  (void) graph;
  (void) buf;
  //reinterpret_cast<Graph*>(graph)->deserialize(buf);
}

void satviz_submit_weight_update(void *graph, CWeightUpdate *update) {
  std::vector<std::tuple<int, int, float>> values { update->n };
  for (size_t i = 0; i < update->n; i++) {
    values.emplace_back(update->from[i], update->to[i], update->weight[i]);
  }
  WeightUpdate realUpdate { values };
  // TODO not implemented yet
  (void) graph;
  (void) realUpdate;
  //reinterpret_cast<Graph*>(graph)->submitWeightUpdate(realUpdate);
}

void satviz_submit_heat_update(void *graph, CHeatUpdate *update) {
  std::vector<std::tuple<int, int>> values { update->n };
  for (size_t i = 0; i < update->n; i++) {
    values.emplace_back(update->index[i], update->heat[i]);
  }
  HeatUpdate realUpdate { values };
  // TODO not implemented yet
  (void) graph;
  (void) realUpdate;
  //reinterpret_cast<Graph*>(graph)->submitHeatUpdate(realUpdate);
}

NodeInfo satviz_query_node(void *graph, int index) {
  (void) graph;
  (void) index;
  // TODO not implemented yet
  // return reinterpret_cast<Graph*>(graph)->queryNode(index);
  return NodeInfo {};
}

EdgeInfo satviz_query_edge(void *graph, int index1, int index2) {
  (void) graph;
  (void) index1;
  (void) index2;
  // TODO not implemented yet
  //return reinterpret_cast<Graph*>(graph)->queryEdge(index1, index2);
  return EdgeInfo {};
}

void *satviz_new_video_controller(void *graph, int display_type) {
  static const int width = 800;
  static const int height = 600;
  satviz::video::Display *display;
  switch (display_type) {
    case 0:
      display = new satviz::video::OffscreenDisplay { width, height };
      break;
    case 1:
      display = new satviz::video::OnscreenDisplay { width, height };
      break;
    default:
      return nullptr;
  }

  return new satviz::video::VideoController { *reinterpret_cast<Graph*>(graph), display };
}

}
