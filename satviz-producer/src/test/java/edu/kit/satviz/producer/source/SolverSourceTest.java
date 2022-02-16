package edu.kit.satviz.producer.source;

import static edu.kit.satviz.producer.SolverParams.solverParams;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.satviz.producer.ResourceHelper;
import edu.kit.satviz.producer.SourceException;
import edu.kit.satviz.producer.mode.SolverMode;
import edu.kit.satviz.sat.SatAssignment;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;

class SolverSourceTest {

  private SolverMode mode;

  @BeforeAll
  static void createTempDir() throws IOException {
    ResourceHelper.createTempDir();
  }

  @AfterAll
  static void deleteTempDir() throws IOException {
    ResourceHelper.deleteTempDir();
  }

  @BeforeEach
  void setUp() {
    mode = new SolverMode();
  }

  @Test
  void test_open_satisfiable() throws IOException, SourceException {
    var params = solverParams("/libcadical.so", "/instance.cnf");
    var source = mode.createSource(params);
    var solution = new AtomicReference<SatAssignment>(null);
    source.whenSolved(solution::set);
    source.open();
    assertNotNull(solution.get());
  }

  @Test
  void test_open_unsatisfiable() throws IOException, SourceException {
    var params = solverParams("/libcadical.so", "/instance-unsat.cnf");
    var source = mode.createSource(params);
    var bool = new AtomicBoolean(false);
    source.whenRefuted(() -> bool.set(true));
    source.open();
    assertTrue(bool.get());
  }

}
