package asura.ui.cli.store

import asura.ui.cli.store.lucene._
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.Sort
import org.apache.lucene.util.IOUtils

object LuceneSpec {

  val lucene = LuceneStore("target/lucene")
  IOUtils.rm(lucene.directory.get)

  val name = lucene.define.field[String]("name", FieldType.UN_TOKENIZED)
  val age = lucene.define.field[Int]("age", FieldType.NUMERIC)
  val intro = lucene.define.field[String]("intro")
  val progress = lucene.define.field[Double]("progress", FieldType.NUMERIC)
  val bytes = lucene.define.field[Long]("bytes", FieldType.NUMERIC)
  val enabled = lucene.define.field[Boolean]("enabled")
  val email = lucene.define.field[String]("email", FieldType.UN_TOKENIZED)

  def main(args: Array[String]): Unit = {
    add()
    query()
  }

  def add(): Unit = {
    lucene.doc()
      .fields(name("a"), age(20), intro("我是中国人, 我爱中国"), progress(7.234), bytes(56789L), enabled(true), email("a@ojlm.tech"))
      .index()
    lucene.doc()
      .fields(name("b"), age(18), intro("我是中国人, 我爱中国"), progress(4.234), bytes(56789L), enabled(true), email("b@ojlm.tech"))
      .index()
    lucene.doc()
      .fields(name("c"), age(30), intro("我是中国人, 我爱中国"), progress(13.234), bytes(56789L), enabled(true), email("c@ojlm.tech"))
      .index()
    lucene.commit()
  }

  def query(): Unit = {
    val results = lucene.query().filter(
      age >= 20, exact(enabled(true)), parse(intro, "中国")
    ).sort(Sort(progress)).search()
    println(s"total: ${results.total}")
    results.entries.foreach(doc => {
      println(s"id: ${doc(lucene.id)} ,name: ${doc(name)}, age: ${doc(age)}, intro: ${doc(intro)}, progress: ${doc(progress)}, email: ${doc(email)}")
    })
  }

}
