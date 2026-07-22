// TRAILING_COMMA_STRATEGY NONE
// ALLOW_TRAILING_WHITESPACE

fun doIt(world: String) {
  println(
      """This line has trailing whitespace         
      world!""")
  println(
      """This line has trailing whitespace $s     
      world!""")
  println(
      """This line has trailing whitespace ${s}   
      world!""")
  println(
      """This line has trailing whitespace $      
      world!""")
}
