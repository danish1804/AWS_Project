let registerEndpoint = "";

window.addEventListener("DOMContentLoaded", async () => {
    try {
        const configResponse = await fetch("config.json");
        const config = await configResponse.json();
        registerEndpoint = config.registerEndpoint;
        console.log("✅ Loaded register endpoint from config:", registerEndpoint);
    } catch (error) {
        console.error("❌ Failed to load config.json", error);
        displayMessage("Failed to load configuration.", true);
        return;
    }

    // 👁️ Toggle password visibility
    document.getElementById("togglePassword").addEventListener("change", function () {
        const passwordField = document.getElementById("password");
        passwordField.type = this.checked ? "text" : "password";
    });


});

function displayMessage(msg, isError = true) {
    const msgBox = document.getElementById("message");
    msgBox.innerText = msg;
    msgBox.style.color = isError ? "red" : "green";
}

function clearMessage() {
    document.getElementById("message").innerText = "";
}

// 🚀 Handle registration
document.getElementById("registerForm").addEventListener("submit", async function (e) {
    e.preventDefault();
    clearMessage();

    const email = document.getElementById("email").value.trim();
    const user_name = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    // ✅ Email format validation
    const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/;
    if (!emailRegex.test(email)) {
        displayMessage("❌ Invalid email format.", true);
        return;
    }

    // ✅ Password strength validation
    const strongPasswordRegex = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$/;
    if (!strongPasswordRegex.test(password)) {
        displayMessage("❌ Password must be at least 8 characters and include uppercase, lowercase, number, and special character.", true);
        return;
    }

    console.log("Submitting payload:", JSON.stringify({ email, user_name, password }));

    try {
        const response = await fetch(registerEndpoint, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, user_name, password })
        });

        const raw = await response.json();
        console.log("Raw Lambda Response:", raw);

        let result;

        try {
            if (raw.body) {
                result = typeof raw.body === "string" ? JSON.parse(raw.body) : raw.body;
            } else {
                result = raw;
            }
        } catch (err) {
            console.error("❌ Failed to parse response body:", raw.body);
            alert("Registration failed due to an invalid response.");
            return;
        }

// 🧠 Defensive check
        if (!result || typeof result.status === "undefined") {
            alert("Malformed response from server.");
            return;
        }

        if (result.status === "fail") {
            alert(result.message || "Registration failed.");
        } else {
            alert(result.message || "Registered successfully.");
            window.location.href = "login.html";
        }

    } catch (err) {
        console.error("❌ Registration error:", err);
        displayMessage("Something went wrong during registration.", true);
    }
});