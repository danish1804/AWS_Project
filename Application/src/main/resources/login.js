let loginEndpoint = "";

window.addEventListener("DOMContentLoaded", async () => {
    try {
        const configResponse = await fetch("config.json");
        const config = await configResponse.json();
        loginEndpoint = config.loginEndpoint;
        console.log("✅ Loaded login endpoint from config:", loginEndpoint);
    } catch (error) {
        console.error("❌ Failed to load config.json", error);
        displayError("Failed to load configuration. Please try again later.");
        return;
    }

    document.getElementById("loginForm").addEventListener("submit", handleLogin);

    document.getElementById("togglePassword").addEventListener("change", function () {
        const passwordField = document.getElementById("password");
        passwordField.type = this.checked ? "text" : "password";
    });
});

async function handleLogin(e) {
    e.preventDefault();
    clearError();

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    if (!email || !password) {
        displayError("Please enter both email and password.");
        return;
    }

    try {
        const response = await fetch(loginEndpoint, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ email, password })
        });

        const raw = await response.json();
        console.log("Raw Lambda Response:", raw);

        let result = raw;
        if (typeof raw.body === "string") {
            result = JSON.parse(raw.body);
        }

        if (result.status === "success") {
            localStorage.setItem("email", email);
            localStorage.setItem("user_name", result.user_name);
            console.log("✅ Login success. Redirecting...");
            window.location.href = "main.html";
        } else {
            displayError(result.message || "Invalid credentials");
        }

    } catch (err) {
        console.error("❌ Error during login:", err);
        displayError("Something went wrong during login.");
    }
}

function displayError(msg) {
    document.getElementById("errorMessage").innerText = msg;
}

function clearError() {
    document.getElementById("errorMessage").innerText = "";
}