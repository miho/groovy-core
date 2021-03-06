package groovy.transform.stc

/**
 * Unit tests for static type checking : bug fixes.
 *
 * @author Cedric Champeau
 */
class BugsSTCTest extends StaticTypeCheckingTestCase {
    // GROOVY-5456
    void testShouldNotAllowDivOnUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { it / 2 } }
        ''', 'Cannot find matching method java.lang.Object#div(int)'
    }
    void testShouldNotAllowDivBynUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { 2 / it } }
        ''', 'Cannot find matching method int#div(java.lang.Object)'
    }
    void testShouldNotAllowModOnUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { it % 2 } }
        ''', 'Cannot find matching method java.lang.Object#mod(int)'
    }
    void testShouldNotAllowModBynUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { 2 % it } }
        ''', 'Cannot find matching method int#mod(java.lang.Object)'
    }
    void testShouldNotAllowMulOnUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { it * 2 } }
        ''', 'Cannot find matching method java.lang.Object#multiply(int)'
    }
    void testShouldNotAllowMulBynUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { 2 * it } }
        ''', 'Cannot find matching method int#multiply(java.lang.Object)'
    }
    void testShouldNotAllowPlusOnUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { it + 2 } }
        ''', 'Cannot find matching method java.lang.Object#plus(int)'
    }
    void testShouldNotAllowPlusWithUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { 2 + it } }
        ''', 'Cannot find matching method int#plus(java.lang.Object)'
    }
    void testShouldNotAllowMinusOnUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { it - 2 } }
        ''', 'Cannot find matching method java.lang.Object#minus(int)'
    }
    void testShouldNotAllowMinusBynUntypedVariable() {
        shouldFailWithMessages '''
            def foo(Closure cls) {}
            def bar() { foo { 2 - it } }
        ''', 'Cannot find matching method int#minus(java.lang.Object)'
    }

    void testGroovy5444() {
        assertScript '''
                def curr = { System.currentTimeMillis() }

                5.times {
                    @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                        assert node.getNodeMetaData(DECLARATION_INFERRED_TYPE) == long_TYPE
                    })
                    def t0 = curr()
                    100000.times {
                        "echo"
                    }
                    println (curr() - t0)
                }'''

    }

    void testGroovy5487ReturnNull() {
        assertScript '''
        @ASTTest(phase=INSTRUCTION_SELECTION, value= {
            assert node.getNodeMetaData(INFERRED_RETURN_TYPE) == make(List)
        })
        List getList() {
            null
        }
        '''
    }

    void testGroovy5487ReturnNullWithExplicitReturn() {
        assertScript '''
        @ASTTest(phase=INSTRUCTION_SELECTION, value= {
            assert node.getNodeMetaData(INFERRED_RETURN_TYPE) == make(List)
        })
        List getList() {
            return null
        }
        '''
    }

    void testGroovy5487ReturnNullWithEmptyBody() {
        assertScript '''
        @ASTTest(phase=INSTRUCTION_SELECTION, value= {
            assert node.getNodeMetaData(INFERRED_RETURN_TYPE) == make(List)
        })
        List getList() {
        }
        '''
    }

    void testGroovy5482ListsAndFlowTyping() {
        assertScript '''
        class StaticGroovy2 {
            def bar() {

                def foo = [new Date(), 1, new C()]
                foo.add( 2 ) // Compiles
                foo.add( new Date() )
                foo.add( new C() )

                foo = [new Date(), 1]
                foo.add( 2 ) // Does not compile
            }
        }
        class C{
        }
        new StaticGroovy2()'''
    }

    void testClosureDelegateThisOwner() {
        assertScript '''
            class A {
                A that = this
                void m() {
                    def cl = {
                        @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                            assert node.getNodeMetaData(INFERRED_TYPE)?.name == 'A'
                        })
                        def foo = this
                        assert this == that
                    }
                    cl()
                    cl = {
                        @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                            assert node.getNodeMetaData(INFERRED_TYPE)?.name == 'A'
                        })
                        def foo = delegate
                        assert delegate == that
                    }
                    cl()
                    cl = {
                        @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                            assert node.getNodeMetaData(INFERRED_TYPE)?.name == 'A'
                        })
                        def foo = owner
                        assert owner == that
                    }
                }
            }
            new A().m()
        '''
    }
    void testClosureDelegateThisOwnerUsingGetters() {
        assertScript '''
            class A {
                A that = this
                void m() {
                    def cl = {
                        @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                            assert node.getNodeMetaData(INFERRED_TYPE)?.name == 'A'
                        })
                        def foo = getThisObject()
                        assert getThisObject() == that
                    }
                    cl()
                    cl = {
                        @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                            assert node.getNodeMetaData(INFERRED_TYPE)?.name == 'A'
                        })
                        def foo = getDelegate()
                        assert getDelegate() == that
                    }
                    cl()
                    cl = {
                        @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                            assert node.getNodeMetaData(INFERRED_TYPE)?.name == 'A'
                        })
                        def foo = getOwner()
                        assert getOwner() == that
                    }
                }
            }
            new A().m()
        '''
    }
}
