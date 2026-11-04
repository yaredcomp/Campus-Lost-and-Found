package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.detail.ItemDetailScreen
import com.example.ui.screens.feed.FeedScreen
import com.example.ui.screens.login.LoginScreen
import com.example.ui.screens.myposts.MyPostsScreen
import com.example.ui.screens.post.PostItemScreen
import com.example.ui.screens.register.RegisterScreen
import com.example.ui.screens.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FEED = "feed"
    const val POST_ITEM = "post_item"
    const val ITEM_DETAIL = "item_detail/{itemId}"
    const val MY_POSTS = "my_posts"

    fun itemDetail(itemId: String) = "item_detail/$itemId"
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToFeed = {
                    navController.navigate(Routes.FEED) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.FEED) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.FEED) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.FEED) {
            FeedScreen(
                onNavigateToPost = {
                    navController.navigate(Routes.POST_ITEM)
                },
                onNavigateToDetail = { itemId ->
                    navController.navigate(Routes.itemDetail(itemId))
                },
                onNavigateToMyPosts = {
                    navController.navigate(Routes.MY_POSTS)
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.FEED) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.POST_ITEM) {
            PostItemScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.ITEM_DETAIL,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            ItemDetailScreen(
                itemId = itemId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MY_POSTS) {
            MyPostsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { itemId ->
                    navController.navigate(Routes.itemDetail(itemId))
                }
            )
        }
    }
}
