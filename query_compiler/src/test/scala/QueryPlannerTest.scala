package scala

import duncecap._
import org.scalatest.FunSuite

class QueryPlannerTest extends FunSuite {
  test("Test that query optimizer can return a single relation in 1 bag") {
    val result = Result(Rel("Simple", Attributes(List("a", "b")), Annotations(List())), false)
    val operation = Operation("*")
    val order =  Order(Attributes(List("a", "b")))
    val project = Project(Attributes(List()))
    val join = Join(List(
      Rel("Edge", Attributes(List("a", "b")), Annotations(List()))))
    val agg = Aggregations(List())
    val filters = Filters(List())
    val rule = Rule(
      result,
      None,
      operation,
      order,
      project,
      join,
      agg,
      filters
    )
    val ir = IR(List(rule))

    val optimized = QueryPlanner.findOptimizedPlans(
      DatalogParser.run("Simple(a,b) :- Edge(a,b)."))
    assertResult(result)(optimized.rules.head.result)
    assertResult(operation)(optimized.rules.head.operation)
    assertResult(order)(optimized.rules.head.order)
    assertResult(project)(optimized.rules.head.project)
    assertResult(join)(optimized.rules.head.join)
    assertResult(rule)(optimized.rules.head)
    assertResult(ir)(optimized)
  }

  test("Test that query optimizer can return triangle query in 1 bag") {
    val ir = IR(List(Rule(
      Result(Rel("Triangle", Attributes(List("a", "b", "c")), Annotations(List())), false),
      None,
      Operation("*"),
      Order(Attributes(List("a", "b", "c"))),
      Project(Attributes(List())),
      Join(List(
        Rel("Edge", Attributes(List("a", "b")), Annotations(List())),
        Rel("Edge", Attributes(List("b", "c")), Annotations(List())),
        Rel("Edge", Attributes(List("a", "c")), Annotations(List())))),
      Aggregations(List()),
      Filters(List())
    )))

    val optimized = QueryPlanner.findOptimizedPlans(DatalogParser.run("Triangle(a,b,c) :- Edge(a,b),Edge(b,c),Edge(a,c)."))
    assertResult(optimized)(ir)
  }

  test("lollipop query") {
    val bag1 = Rule(
      Result(Rel("bag_1_a_b_c_Lollipop", Attributes(List("a", "b", "c")), Annotations(List())), true),
      None,
      Operation("*"),
      Order(Attributes(List("a", "b", "c"))),
      Project(Attributes(List())),
      Join(List(
        Rel("Edge", Attributes(List("a", "b")), Annotations(List())),
        Rel("Edge", Attributes(List("b", "c")), Annotations(List())),
        Rel("Edge", Attributes(List("a", "c")), Annotations(List())))),
      Aggregations(List()),
      Filters(List())
    )
    val bag0 = Rule(
      Result(Rel("Lollipop_root", Attributes(List("a", "d")), Annotations(List())), true),
      None,
      Operation("*"),
      Order(Attributes(List("a", "d", "b", "c"))),
      Project(Attributes(List("b", "c"))),
      Join(List(
        Rel("Edge", Attributes(List("a", "d")), Annotations(List())),
        Rel("bag_1_a_b_c_Lollipop", Attributes(List("a", "b", "c")), Annotations(List())))),
      Aggregations(List()),
      Filters(List())
    )

    val topdownPass = Rule(
      Result(Rel("Lollipop", Attributes(List("a", "b", "c", "d")), Annotations(List())), false),
      None,
      Operation("*"),
      Order(Attributes(List("a", "d", "b", "c"))),
      Project(Attributes(List())),
      Join(List(
        Rel("Lollipop_root", Attributes(List("a", "d")), Annotations(List())),
        Rel("bag_1_a_b_c_Lollipop", Attributes(List("a", "b", "c")), Annotations(List()))
      )),
      Aggregations(List()),
      Filters(List())
    )

    val optimized = QueryPlanner.findOptimizedPlans(DatalogParser.run("Lollipop(a,b,c,d) :- Edge(a,b),Edge(b,c),Edge(a,c),Edge(a,d)."))
    assertResult(IR(List(bag1, bag0, topdownPass)))(optimized)
  }

  test("barbell query") {
    val bag2 = Rule(
      Result(Rel("bag_1_d_e_f_Barbell", Attributes(List("d", "e", "f")), Annotations(List())), true),
      None,
      Operation("*"),
      Order(Attributes(List("d", "e", "f"))),
      Project(Attributes(List())),
      Join(List(
        Rel("Edge", Attributes(List("d", "f")), Annotations(List())),
        Rel("Edge", Attributes(List("e", "f")), Annotations(List())),
        Rel("Edge", Attributes(List("d", "e")), Annotations(List()))
      )),
      Aggregations(List()),
      Filters(List())
    )

    val bag1 = Rule(
      Result(Rel("bag_1_a_b_c_Barbell", Attributes(List("a", "b", "c")), Annotations(List())), true),
      None,
      Operation("*"),
      Order(Attributes(List("a", "b", "c"))),
      Project(Attributes(List())),
      Join(List(
        Rel("Edge", Attributes(List("a", "c")), Annotations(List())),
        Rel("Edge", Attributes(List("b", "c")), Annotations(List())),
        Rel("Edge", Attributes(List("a", "b")), Annotations(List()))
      )),
      Aggregations(List()),
      Filters(List())
    )

    val bag0 = Rule(
      Result(Rel("Barbell_root", Attributes(List("a", "d")), Annotations(List())), true),
      None,
      Operation("*"),
      Order(Attributes(List("a", "d", "e", "f", "b", "c"))),
      Project(Attributes(List("b", "c", "e", "f"))),
      Join(List(
        Rel("Edge", Attributes(List("a", "d")), Annotations(List())),
        Rel("bag_1_a_b_c_Barbell", Attributes(List("a", "b", "c")), Annotations(List())),
        Rel("bag_1_d_e_f_Barbell", Attributes(List("d", "e", "f")), Annotations(List())))),
      Aggregations(List()),
      Filters(List())
    )

    val topdownPass = Rule(
      Result(Rel("Barbell", Attributes(List("a", "b", "c", "d", "e","f")), Annotations(List())), false),
      None,
      Operation("*"),
      Order(Attributes(List("a", "d", "e", "f", "b", "c"))),
      Project(Attributes(List())),
      Join(List(
        Rel("Barbell_root", Attributes(List("a", "d")), Annotations(List())),
        Rel("bag_1_a_b_c_Barbell", Attributes(List("a", "b", "c")), Annotations(List())),
        Rel("bag_1_d_e_f_Barbell", Attributes(List("d", "e", "f")), Annotations(List()))
      )),
      Aggregations(List()),
      Filters(List())
    )

    val optimized = QueryPlanner.findOptimizedPlans(DatalogParser.run(
      "Barbell(a,b,c,d,e,f) :- Edge(a,b),Edge(b,c),Edge(a,c),Edge(d,e),Edge(e,f),Edge(d,f),Edge(a,d)."))
    assertResult(topdownPass)(optimized.rules(3))
    assertResult(bag0)(optimized.rules(2))
    assertResult(bag1)(optimized.rules(1))
    assertResult(bag2)(optimized.rules(0))
    assertResult(IR(List(bag2, bag1, bag0, topdownPass)))(optimized)
  }

  test("lollipop agg") {
    val ir = IR(List(
      Rule(Result(
        Rel("bag_1_a_b_c_Lollipop",Attributes(List("a")),Annotations(List("z"))), true),None,Operation("*"),Order(Attributes(List("a", "b", "c"))),Project(Attributes(List())),
        Join(List(
          Rel("Edge",Attributes(List("a", "c")),Annotations(List())),
          Rel("Edge",Attributes(List("b", "c")),Annotations(List())),
          Rel("Edge",Attributes(List("a", "b")),Annotations(List())))),
        Aggregations(List(Aggregation("z","long",SUM(),Attributes(List("b", "c")),"1","AGG"))),Filters(List())),
      Rule(Result(
        Rel("Lollipop",Attributes(List()),Annotations(List("z"))), false),None,Operation("*"),Order(Attributes(List("a", "d"))),Project(Attributes(List())),
        Join(List(
          Rel("Edge",Attributes(List("a", "d")),Annotations(List())),
          Rel("bag_1_a_b_c_Lollipop",Attributes(List("a")),Annotations(List("z"))))),
        Aggregations(List(Aggregation("z","long",SUM(),Attributes(List("a", "d")),"1","AGG"))),Filters(List()))
      ))
    val optimized = QueryPlanner.findOptimizedPlans(DatalogParser.run("Lollipop(;z) :- Edge(a,b),Edge(b,c),Edge(a,c),Edge(a,d),z:uint64<-[COUNT(*)]"))
    assertResult(ir)(optimized)
  }

  test("lollipop partial agg with project") {
    val ir = IR(List(
      Rule(Result(
        Rel("bag_1_a_d_Lollipop",Attributes(List("a")),Annotations(List("z"))), true),None,Operation("*"),Order(Attributes(List("a", "d"))),Project(Attributes(List())),
        Join(List(
          Rel("Edge", Attributes(List("a", "d")),Annotations(List())))),
        Aggregations(List(Aggregation("z","long",SUM(),Attributes(List("d")),"1","AGG"))),Filters(List())),
      Rule(Result(
        Rel("Lollipop",Attributes(List("a")),Annotations(List("z"))), false),None,Operation("*"),Order(Attributes(List("a", "b", "c"))),Project(Attributes(List("b"))),
        Join(List(
          Rel("Edge",Attributes(List("a", "c")),Annotations(List())),
          Rel("Edge",Attributes(List("b", "c")),Annotations(List())),
          Rel("Edge",Attributes(List("a", "b")),Annotations(List())),
          Rel("bag_1_a_d_Lollipop",Attributes(List("a")),Annotations(List("z"))))),
        Aggregations(List(Aggregation("z","long",SUM(),Attributes(List("c")),"1","AGG"))),Filters(List()))))
    val optimized = QueryPlanner.findOptimizedPlans(DatalogParser.run(
      "Lollipop(a;z) :- Edge(a,b),Edge(b,c),Edge(a,c),Edge(a,d),z:uint64<-[COUNT(c,d)]"))
    assertResult(ir)(optimized)
  }

  test("4 clique with selection and aggregation") {
    val ir = IR(List(
      Rule(Result(
        Rel("bag_1_x_a_FliqueSelAgg",Attributes(List("a")),Annotations(List("z"))),true),
        None,
        Operation("*"),
        Order(Attributes(List("x", "a"))),
        Project(Attributes(List("x"))),
        Join(List(Rel("Edge",Attributes(List("a", "x")),Annotations(List())))),
        Aggregations(List()),Filters(List(Selection("x",EQUALS(),"0")))),
      Rule(Result(
        Rel("FliqueSelAgg",Attributes(List()),Annotations(List("z"))),false),
        None,
        Operation("*"),
        Order(Attributes(List("a", "b", "c", "d"))),
        Project(Attributes(List())),
        Join(List(
          Rel("Edge",Attributes(List("c", "d")),Annotations(List())),
          Rel("Edge",Attributes(List("b", "c")),Annotations(List())),
          Rel("Edge",Attributes(List("b", "d")),Annotations(List())),
          Rel("Edge",Attributes(List("a", "c")),Annotations(List())),
          Rel("bag_1_x_a_FliqueSelAgg",Attributes(List("a")),Annotations(List("z"))),
          Rel("Edge",Attributes(List("a", "d")),Annotations(List())),
          Rel("Edge",Attributes(List("a", "b")),Annotations(List())))),
        Aggregations(List(Aggregation("z","long",SUM(),Attributes(List("a", "b", "c", "d")),"1","AGG"))),Filters(List()))
      ))

    val optimized = QueryPlanner.findOptimizedPlans(DatalogParser.run(
      "FliqueSelAgg(;z) :- Edge(a,b),Edge(b,c),Edge(a,c),Edge(a,d),Edge(b,d),Edge(c,d),Edge(a,x),x=0,z:uint64<-[COUNT(*)]."))
    assertResult(ir)(optimized)
    //println(optimized)
  }

  test("Barbell with selection") {
    // TODO
    val optimized = QueryPlanner.findOptimizedPlans(DatalogParser.run("""
        BarbellSel(a,b,c,x,y,z) :- Edge(a,b),Edge(b,c),Edge(a,c),Edge(a,p),Edge(p,x),Edge(x,y),Edge(y,z),Edge(x,z),p=0.
    """))
    println(optimized)
  }
}
