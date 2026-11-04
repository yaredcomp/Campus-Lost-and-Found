package com.example

import com.example.data.repository.ItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticatedUser_cannotCreateItem() = runBlocking {
        auth.signOut()
        val repository = ItemRepository(firestore)

        val result = repository.createItem(
            type = "LOST",
            title = "Keys",
            description = "Keys with red lanyard",
            location = "Library",
            imageUrl = ""
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun authenticatedUser_canCreateAndReadItem() = runBlocking {
        val aliceUid = signInTestUser("alice_rule_test@campus.edu")
        val repository = ItemRepository(firestore)

        val createResult = repository.createItem(
            type = "LOST",
            title = "Calculus Textbook",
            description = "Stewart 9th edition left in lecture hall",
            location = "Hall A",
            imageUrl = "https://example.com/book.jpg"
        )

        assertTrue(createResult.isSuccess)
        val itemId = createResult.getOrThrow()
        assertNotNull(itemId)

        // Read single item
        val item = repository.observeItemById(itemId).first()
        assertNotNull(item)
        assertEquals("Calculus Textbook", item?.title)
        assertEquals(aliceUid, item?.ownerId)
        assertEquals("LOST", item?.type)
    }

    @Test
    fun unauthorizedUser_cannotDeleteItem() = runBlocking {
        val aliceUid = signInTestUser("alice_author@campus.edu")
        val repository = ItemRepository(firestore)

        val createResult = repository.createItem(
            type = "FOUND",
            title = "Water Bottle",
            description = "Hydroflask",
            location = "Gym",
            imageUrl = ""
        )
        val itemId = createResult.getOrThrow()

        // Sign in as Bob
        signInTestUser("bob_intruder@campus.edu")

        val deleteResult = repository.deleteItem(itemId, aliceUid)
        assertTrue(deleteResult.isFailure)
    }
}
