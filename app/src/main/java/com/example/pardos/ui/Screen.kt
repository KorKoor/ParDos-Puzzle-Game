package com.example.pardos.ui

sealed class Screen {
    object Menu : Screen()
    object Game : Screen()
    object CustomLevel : Screen()
    object Records : Screen()
}