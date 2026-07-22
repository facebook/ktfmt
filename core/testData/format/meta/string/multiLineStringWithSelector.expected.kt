val STRING =
    """
    |foo
    |"""
        .wouldFit()

val STRING =
    """
    |foo
    |//////////////////////////////////"""
        .wouldntFit()

val STRING =
    """
    |foo
    |"""
        .firstLink()
        .secondLink()
