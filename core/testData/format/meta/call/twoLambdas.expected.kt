fun f() {
  doIt()
      .apply {
        number =
            computeNumber1()
      }
      .apply {
        number = 2 * number
      }
}
