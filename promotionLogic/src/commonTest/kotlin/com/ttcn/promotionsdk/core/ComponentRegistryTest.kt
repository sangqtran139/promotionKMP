package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.di.internal.ComponentRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Engine DI tự viết. **Không sửa** engine (xem DependencyInjection.md) — chỉ khoá hành vi để
 * refactor sau này không đổi ngầm: single dùng lại instance, factory tạo mới, qualifier tách khoá.
 */
class ComponentRegistryTest {

    private class Foo(val tag: String = "x")
    private class Bar

    @Test
    fun single_returnsSameInstanceEveryResolve() {
        val r = ComponentRegistry()
        var built = 0
        r.single { built++; Foo() }

        val a = r.resolve(Foo::class)
        val b = r.resolve(Foo::class)
        assertSame(a, b)
        assertEquals(1, built)   // provider chỉ chạy một lần
    }

    @Test
    fun factory_buildsNewInstanceEveryResolve() {
        val r = ComponentRegistry()
        var built = 0
        r.factory { built++; Foo() }

        val a = r.resolve(Foo::class)
        val b = r.resolve(Foo::class)
        assertNotSame(a, b)
        assertEquals(2, built)
    }

    @Test
    fun resolve_missingDependency_throwsWithReadableMessage() {
        val r = ComponentRegistry()
        val e = assertFailsWith<IllegalStateException> { r.resolve(Bar::class) }
        assertTrue(e.message!!.contains("Bar"))
    }

    @Test
    fun resolve_missingWithQualifier_mentionsQualifierInMessage() {
        val r = ComponentRegistry()
        val e = assertFailsWith<IllegalStateException> { r.resolve(Bar::class, qualifier = "q1") }
        assertTrue(e.message!!.contains("q1"))
    }

    @Test
    fun qualifier_separatesRegistrationsOfSameType() {
        val r = ComponentRegistry()
        r.single(qualifier = "a") { Foo("A") }
        r.single(qualifier = "b") { Foo("B") }

        assertEquals("A", r.resolve(Foo::class, "a").tag)
        assertEquals("B", r.resolve(Foo::class, "b").tag)
    }

    @Test
    fun contain_isTrueRightAfterRegister_butHasInstanceOnlyAfterResolve() {
        val r = ComponentRegistry()
        r.single { Foo() }

        assertTrue(r.contain(Foo::class))
        assertFalse(r.hasInstance(Foo::class))   // chưa dựng

        r.resolve(Foo::class)
        assertTrue(r.hasInstance(Foo::class))
    }

    @Test
    fun contain_isFalseForUnregisteredTypeAndWrongQualifier() {
        val r = ComponentRegistry()
        r.single(qualifier = "a") { Foo() }

        assertFalse(r.contain(Bar::class))
        assertFalse(r.contain(Foo::class, qualifier = "khac"))
    }

    @Test
    fun reRegister_overridesPreviousProvider() {
        val r = ComponentRegistry()
        r.single { Foo("cu") }
        r.single { Foo("moi") }
        assertEquals("moi", r.resolve(Foo::class).tag)
    }

    @Test
    fun reRegisterAfterResolve_keepsStaleCachedInstance_untilCleared() {
        // Bất biến mà `PromotionSDK.updateToken` DỰA VÀO: re-register single sau khi đã resolve
        // KHÔNG thay instance đã cache — phải `clear()` trước. Đây chính là lý do updateToken gọi
        // `PromotionContainer.clear()` trước khi re-init (nếu không, HttpClient token cũ vẫn sống).
        val r = ComponentRegistry()
        r.single { Foo("cu") }
        assertEquals("cu", r.resolve(Foo::class).tag)   // dựng + cache

        r.single { Foo("moi") }                          // override provider…
        assertEquals("cu", r.resolve(Foo::class).tag)   // …nhưng instance cache vẫn CŨ

        r.clear()
        r.single { Foo("moi") }
        assertEquals("moi", r.resolve(Foo::class).tag)   // sau clear mới lấy được instance mới
    }
}
