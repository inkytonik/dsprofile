/**
 * This file is part of dsprofile.
 *
 * Copyright (C) 2012-2014 Anthony M Sloane, Macquarie University.
 * Copyright (C) 2012-2014 Matthew Roberts, Macquarie University.
 *
 * dsprofile is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * dsprofile is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with dsprofile.  (See files COPYING and COPYING.LESSER.)  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.bitbucket.inkytonik.dsprofile

import Events.{start, finish}

import org.scalatest.{BeforeAndAfter, FunSuiteLike}
import org.scalatest.Matchers

class ProfilerTests extends Profiler
                    with FunSuiteLike
                    with BeforeAndAfter
                    with Matchers {

    import scala.collection.immutable.Seq

    var otp = ""
    override def output (str : String) : Unit = {otp = otp + str}
    override def outputln (str : String) : Unit = {otp = otp + str + "\n"}

    val countShouldBe = colShouldBe (6) _
    val totalShouldBe = colShouldBe (0) _

    // FIXME: Can't make use of default parameter, compiler complains for reasons I don't understand.
    // FIXME: I think it's because you can't have default param values on functions, just methods
    def colShouldBe (col : Int) (num : Int, dim : String, inp : String, within : Double = 0.0) = {
        val lines = inp.split ("\n")
        lines.foreach { l =>
            val v = l.split ("""\s+""")
            if (v.length > 8 && v (8) == dim) {
                assertResult (v (col).toDouble < (num + within), "under upper bound") (true)
                assertResult (v (col).toDouble > (num - within), "above lower bound") (true)
            }
        }
    }

    after {
        Events.reset
    }

    test ("check simple result") {
        otp = ""
        profileStart ()

        val i = start (Seq ("won" -> 1, "too" -> 2))
        Thread.sleep (250)
        finish (i)

        val j = start (Seq ("won" -> 10, "too" -> 20))
        Thread.sleep (250)
        finish (j)

        profileStop (Seq ("won"))

        List ("1", "10").foreach {d =>
            countShouldBe (1, d, otp, 0.0)
            totalShouldBe (250, d, otp, 5.0)
        }
    }

    test ("check slightly more complex result (test b)") {
        otp = ""
        profileStart ()
        val i = start (Seq ("won" -> 1, "too" -> 2))
        Thread.sleep (250)
        finish (i)
        val j = start (Seq ("won" -> 1, "too" -> 20))
        Thread.sleep (750)
        finish (j)
        val k = start (Seq ("won" -> 1, "too" -> 20))
        Thread.sleep (750)
        finish (k)
        profileStop (Seq ("won"))

        countShouldBe (3, "1", otp, 0.0)
        totalShouldBe (1750, "1", otp, 5.0)
    }

    test ("test b on other dimension") {
        otp = ""
        profileStart ()
        val i = start (Seq ("won" -> 1, "too" -> 2))
        Thread.sleep (250)
        finish (i)
        val k = start (Seq ("won" -> 1, "too" -> 20))
        Thread.sleep (750)
        finish (k)
        val j = start (Seq ("won" -> 1, "too" -> 20))
        Thread.sleep (750)
        finish (j)
        profileStop (Seq ("too"))

        countShouldBe (1, "2", otp, 0.0)
        totalShouldBe (250, "2", otp, 5.0)

        countShouldBe (2, "20", otp, 0.0)
        totalShouldBe (1500, "20", otp, 5.0)
    }

    test ("attribute-like profile") {
        otp = ""
        case class AttrEval ()
        profileStart ()
        val i = start (Seq ("event" -> AttrEval, "subject" -> "Use(int)", "attribute" -> "decl", "parameter" -> None))
        val j = start (Seq ("event" -> AttrEval, "subject" -> "Use(int)", "attribute" -> "lookup", "parameter" -> Some("int")))
        val k = start (Seq ("event" -> AttrEval, "subject" -> "VarDecl(Use(int),y)", "attribute" -> "lookup", "parameter" -> Some("int")))
        val l = start (Seq ("event" -> AttrEval, "subject" -> "VarDecl(Use(int),y)", "attribute" -> "declarationOf", "parameter" -> Some("int")))
        finish (l, Seq ("value"->null, "cached" -> false))
        val m = start (Seq ("event" -> AttrEval, "subject" -> "VarDecl(Use(AA),a)", "attribute" -> "declarationOf", "parameter" -> Some("int")))
        finish (m, Seq ("value" -> null, "cached" -> false))
        finish (k, Seq ("value" -> null, "cached" -> false))
        finish (j, Seq ("value" -> null, "cached" -> true))
        finish (i, Seq ("value" -> null, "cached" -> true))
        val reporter = profileStop ()
        reporter (Seq ("event"))
        reporter (Seq ("attribute"))
        countShouldBe (1, "decl", otp, 0.0)
        countShouldBe (2, "lookup", otp, 0.0)
        countShouldBe (2, "declarationOf", otp, 0.0)
        countShouldBe (5, "AttrEval", otp, 0.0)
    }

    test ("that the finish dimension values override the start dimension values") {
        otp = ""
        profileStart ()
        val i = start (Seq ("subject" -> "Use(int)", "attribute" -> "decl", "parameter" -> None))
        val j = start (Seq ("subject" -> "Use(int)", "attribute" -> "lookup", "parameter" -> Some("int")))
        val k = start (Seq ("subject" -> "VarDecl(Use(int),y)", "attribute" -> "lookup", "parameter" -> Some("int")))
        val l = start (Seq ("subject" -> "VarDecl(Use(int),y)", "attribute" -> "declarationOf", "parameter" -> Some("int")))
        finish (l, Seq ("value"->null, "cached" -> false))
        val m = start (Seq ("subject" -> "VarDecl(Use(AA),a)", "attribute" -> "declarationOf", "parameter" -> Some("int")))
        finish (m, Seq ("value" -> null, "cached" -> false))
        finish (k, Seq ("value" -> null, "cached" -> false))
        finish (j, Seq ("value" -> null, "cached" -> true))
        // here we change the attribute dimension of the i event so that there are now three lookup attributes
        finish (i, Seq ("attribute" -> "lookup", "value" -> null, "cached" -> true))
        val reporter = profileStop ()
        reporter (Seq ("attribute"))
        countShouldBe (3, "lookup", otp, 0.0)
    }

    test ("traces contain appropriate events") {
        case class AttrEval ()
        profileStart ()
        val i = start (Seq ("event" -> AttrEval, "subject" -> "Use(int)", "attribute" -> "decl", "parameter" -> None))
        val j = start (Seq ("event" -> AttrEval, "subject" -> "Use(int)", "attribute" -> "lookup", "parameter" -> Some("int")))
        val k = start (Seq ("event" -> AttrEval, "subject" -> "VarDecl(Use(int),y)", "attribute" -> "lookup", "parameter" -> Some("int")))
        val l = start (Seq ("event" -> AttrEval, "subject" -> "VarDecl(Use(int),y)", "attribute" -> "declarationOf", "parameter" -> Some("int")))
        finish (l, Seq ("value"->null, "cached" -> false))
        val m = start (Seq ("event" -> AttrEval, "subject" -> "VarDecl(Use(AA),a)", "attribute" -> "declarationOf", "parameter" -> Some("int")))
        finish (m, Seq ("value" -> null, "cached" -> false))
        finish (k, Seq ("value" -> null, "cached" -> false))
        finish (j, Seq ("value" -> null, "cached" -> true))
        finish (i, Seq ("value" -> null, "cached" -> true))
        profileStop ()

        otp = ""
        trace ()
        val fullTrace = """    1: Start    AttrEval (attribute,decl) (parameter,None) (subject,Use(int))
            |    2: Start    AttrEval (attribute,lookup) (parameter,Some(int)) (subject,Use(int))
            |    3: Start    AttrEval (attribute,lookup) (parameter,Some(int)) (subject,VarDecl(Use(int),y))
            |    4: Start    AttrEval (attribute,declarationOf) (parameter,Some(int)) (subject,VarDecl(Use(int),y))
            |    4: Finish            (cached,false) (value,null)
            |    5: Start    AttrEval (attribute,declarationOf) (parameter,Some(int)) (subject,VarDecl(Use(AA),a))
            |    5: Finish            (cached,false) (value,null)
            |    3: Finish            (cached,false) (value,null)
            |    2: Finish            (cached,true) (value,null)
            |    1: Finish            (cached,true) (value,null)
            |""".stripMargin
        assertResult (fullTrace, "full trace") (otp)

        otp = ""
        trace (_.kind == Events.Start)
        val startTrace = """    1: Start    AttrEval (attribute,decl) (parameter,None) (subject,Use(int))
            |    2: Start    AttrEval (attribute,lookup) (parameter,Some(int)) (subject,Use(int))
            |    3: Start    AttrEval (attribute,lookup) (parameter,Some(int)) (subject,VarDecl(Use(int),y))
            |    4: Start    AttrEval (attribute,declarationOf) (parameter,Some(int)) (subject,VarDecl(Use(int),y))
            |    5: Start    AttrEval (attribute,declarationOf) (parameter,Some(int)) (subject,VarDecl(Use(AA),a))
            |""".stripMargin
        assertResult (startTrace, "start trace") (otp)
    }

}
