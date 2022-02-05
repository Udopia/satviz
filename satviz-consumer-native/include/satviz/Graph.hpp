#ifndef SATVIZ_GRAPH_HPP_
#define SATVIZ_GRAPH_HPP_

#include <vector>
#include <tuple>
#include <sstream>

#include <satviz/info.h>
#include <ogdf/basic/Graph.h>
#include <ogdf/basic/GraphAttributes.h>

namespace satviz {
namespace graph {

class GraphObserver;

struct WeightUpdate {
  std::vector<std::tuple<int, int, float> > values;
};

struct HeatUpdate {
  std::vector<std::tuple<int, int> > values;
};

/**
 *
 */
class Graph {
private:
  ogdf::Graph graph;
  ogdf::GraphAttributes attrs;
  std::vector<GraphObserver*> observers;

public:
  Graph(size_t num_nodes = 0);
  /**
   * This variant of the constructor only exists to aid
   * debugging & testing. It should not be used directly.
   */
  Graph(ogdf::Graph &graphToCopy);

  ogdf::Graph &getOgdfGraph() { return graph; }
  ogdf::GraphAttributes &getOgdfAttrs() { return attrs; }

  void addObserver(GraphObserver *o) { observers.push_back(o); }

  void submitWeightUpdate(WeightUpdate &update);
  void submitHeatUpdate(HeatUpdate &update);

  void recalculateLayout();
  void adaptLayout();

  std::stringbuf serialize();
  void deserialize(std::stringbuf &buf);

  NodeInfo queryNode(int index);
  EdgeInfo queryEdge(int index1, int index2);

  double getX(ogdf::node v) { return attrs.x(v); }
  double getY(ogdf::node v) { return attrs.y(v); }
};

} // namespace graph
} // namespace satviz

#endif
