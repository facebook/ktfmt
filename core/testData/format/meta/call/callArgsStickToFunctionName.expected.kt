class f {
  private val somePropertyWithBackingOne
    get() =
        _somePropertyWithBackingOne
            ?: Classname.getStuff<SomePropertyRelatedClassProvider>(requireContext())[
                    somePropertiesProvider, somePropertyCallbacks]
                .also { _somePropertyWithBackingOne = it }
}
