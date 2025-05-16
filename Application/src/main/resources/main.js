let getSubscriptionsEndpoint = "";
let unsubscribeEndpoint = "";
let subscribeEndpoint = "";
let searchMusicEndpoint = "";
let subscribedSet = new Set();
let isSubscribing = false;
window.addEventListener("DOMContentLoaded", async () => {
    try {
        const configResponse = await fetch("config.json");
        const config = await configResponse.json();
        getSubscriptionsEndpoint = config.getSubscriptionsEndpoint;
        unsubscribeEndpoint = config.unsubscribeEndpoint;
        subscribeEndpoint = config.subscribeEndpoint;
        searchMusicEndpoint = config.searchMusicEndpoint;

        console.log("✅ Loaded endpoints:", config);

        const email = localStorage.getItem("email");
        const userName = localStorage.getItem("user_name");

        if (!email || !userName) {
            showToast("User not logged in. Redirecting...");
            window.location.href = "login.html";
            return;
        }

        document.getElementById("userName").innerText = userName;

        await fetchSubscriptions(email);

    } catch (err) {
        console.error("❌ Error loading dashboard:", err);
        showToast("Error loading dashboard. Please try again.");
    }

    document.getElementById("searchForm").addEventListener("submit", async (e) => {
        e.preventDefault();

        const email = localStorage.getItem("email");
        const title = document.getElementById("title").value.trim();
        const artist = document.getElementById("artist").value.trim();
        const album = document.getElementById("album").value.trim();
        const year = document.getElementById("year").value.trim();

        // Optional validation
        if (!title && !artist && !album && !year) {
            showToast("Please enter at least one field to search.");
            return;
        }

        // UI feedback
        const resultsContainer = document.getElementById("searchResultsContainer");
        resultsContainer.innerHTML = "<p>🔍 Searching songs...</p>";

        try {
            const queryParams = new URLSearchParams({
                title: title || "",
                artist: artist || "",
                album: album || "",
                year: year || ""
            }).toString();

            const url = `${searchMusicEndpoint}?${queryParams}`;

            const res = await fetch(url, {
                method: "GET",
                headers: { "Content-Type": "application/json" }
            });

            const raw = await res.json();
            console.log("🔍 Raw Lambda Response:", raw);

            let result;
            try {
                result = raw.body ?
                    (typeof raw.body === "string" ? JSON.parse(raw.body) : raw.body)
                    : raw;
            } catch (e) {
                console.error("❌ Failed to parse response body:", raw.body);
                resultsContainer.innerHTML = "<p>❌ Failed to parse response.</p>";
                return;
            }

            if (result.status === "success") {
                const subscribedTitles = await fetchSubscriptions(email);
                window.lastSearchResults = result.results;
                renderSearchResults(result.results, subscribedTitles);
                // ✅ Clear the form fields after rendering results
                //document.getElementById("title").value = "";
                //document.getElementById("artist").value = "";
                // document.getElementById("album").value = "";
                // document.getElementById("year").value = "";
            } else {
                resultsContainer.innerHTML = "<p>❌ " + (result.message || "Search failed.") + "</p>";
            }

        } catch (err) {
            console.error("❌ Error during search:", err);
            resultsContainer.innerHTML = "<p>❌ Network or server error during search.</p>";
        }
        setTimeout(() => {
            document.getElementById("searchForm").reset();
        }, 100);
        // ✅ clears all inputs

    });
});

async function fetchSubscriptions(email) {
    try {

        const url = `${getSubscriptionsEndpoint}?email=${encodeURIComponent(email)}`;
        const response = await fetch(url, {
            method: "GET",
            headers: { "Content-Type": "application/json" }
        });

        const raw = await response.json();
        console.log("📦 Raw Lambda Response:", raw);

        let result = raw.body ? JSON.parse(raw.body) : raw;

        if (result.status === "success") {
            subscribedSet = new Set(result.subscriptions.map(s => `${s.title}#${s.album}`));
            renderSubscribedMusic(result.subscriptions);
            return Array.from(subscribedSet); // 👈 For search comparison
        } else {
            showToast(result.message || "Failed to load subscriptions.");
            return [];
        }
    } catch (error) {
        console.error("❌ Failed to fetch subscriptions:", error);
        showToast("Error loading subscriptions.");
        return [];
    }
}

function renderSubscribedMusic(subscriptions) {
    const container = document.getElementById("subscribedMusicContainer");
    container.innerHTML = "";

    if (!subscriptions || subscriptions.length === 0) {
        container.innerHTML = "<p>You haven’t subscribed to any songs yet.</p>";
        return;
    }

    subscriptions.forEach(song => {
        const card = document.createElement("div");
        card.classList.add("card");
        card.style.border = "1px solid #ccc";
        card.style.padding = "10px";
        card.style.marginBottom = "10px";
        card.style.borderRadius = "8px";
        card.style.boxShadow = "0 2px 6px rgba(0,0,0,0.1)";
        card.style.transition = "opacity 0.3s ease-in-out";

        // 📄 Add song text details
        const titleEl = document.createElement("strong");
        titleEl.innerText = song.title;

        const meta = document.createElement("div");
        meta.innerHTML = `by ${song.artist}<br>Album: ${song.album} (${song.year})<br>`;
        card.appendChild(titleEl);
        card.appendChild(meta);

        // 📷 Add song image if available
        if (song.image_s3_url) {
            const img = document.createElement("img");
            img.src = song.image_s3_url;
            img.alt = song.title;
            img.loading = "lazy";
            img.classList.add("song-image");
            img.style.width = "150px";
            img.style.marginTop = "10px";
            img.style.borderRadius = "6px";
            img.style.objectFit = "cover";
            img.style.opacity = "0";
            img.style.transition = "opacity 0.5s ease-in-out";

            img.onload = () => {
                img.style.opacity = "1";
            };

            img.onerror = function() {
                this.onerror = null;
                this.src = "no-image.jpg";
            };

            card.appendChild(img);
        }

        // 🔘 Add Unsubscribe button
        const button = document.createElement("button");
        button.innerText = "Unsubscribe";
        button.addEventListener("click", () => {
            unsubscribe(song.title, song.album);
        });
        button.style.marginTop = "10px";
        card.appendChild(button);

        container.appendChild(card);
    });
}



async function unsubscribe(title, album) {
    const email = localStorage.getItem("email");
    const messageBox = document.getElementById("unsubscribeMessage"); // Add this in HTML
    const titleAlbum = `${title}#${album}`;

    if (!email || !title || !album) {
        showMessage("❌ Invalid unsubscribe request.", true);
        return;
    }

    try {
        showMessage("⏳ Unsubscribing...", false);

        const response = await fetch(unsubscribeEndpoint, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, title, album })
        });

        const raw = await response.json();
        const result = raw.body ? JSON.parse(raw.body) : raw;

        if (result.status === "success") {
            showMessage("✅ Unsubscribed successfully!", false);
            subscribedSet.delete(titleAlbum); // 💡 remove from cache

            // 🔄 Update search result DOM
            const card = document.querySelector(`[data-key="${titleAlbum}"]`);
            if (card) {
                const label = card.querySelector(".subscribed-label");
                const button = document.createElement("button");
                button.innerText = "Subscribe";
                button.addEventListener("click", () => subscribe(title, album));
                label.replaceWith(button); // Swap label with subscribe button
            }

            // 🔄 Refresh subscribed music section
            await fetchSubscriptions(email);
        } else {
            showMessage(result.message || "❌ Failed to unsubscribe.", true);
        }

    } catch (err) {
        console.error("❌ Error during unsubscribe:", err);
        showMessage("❌ Something went wrong during unsubscription.", true);
    }

    // UI message helper
    function showMessage(msg, isError = true) {
        if (!messageBox) return;
        messageBox.innerText = msg;
        messageBox.style.color = isError ? "red" : "green";
        messageBox.style.fontWeight = "bold";
    }
}

function showToast(message, isError = false) {
    const toast = document.createElement("div");
    toast.innerText = message;
    toast.className = "toast";
    toast.style.backgroundColor = isError ? "#e74c3c" : "#2ecc71"; // Red or Green

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.classList.add("visible");
    }, 100);

    setTimeout(() => {
        toast.classList.remove("visible");
        setTimeout(() => document.body.removeChild(toast), 300);
    }, 3000);
}


function renderSearchResults(songs, subscribedTitles) {
    const container = document.getElementById("searchResultsContainer");
    container.innerHTML = "";

    if (!songs.length) {
        container.innerHTML = "<p>No matching songs found.</p>";
        return;
    }

    console.log("🎨 Rendering search results:", songs);

    songs.forEach(song => {
        console.log(`🖼️ Song: ${song.title}, image_s3_url: ${song.image_s3_url}`);
        const key = `${song.title}#${song.album}`;

        const card = document.createElement("div");
        card.classList.add("card");
        card.style.border = "1px solid #ccc";
        card.style.padding = "10px";
        card.style.marginBottom = "10px";
        card.style.borderRadius = "8px";
        card.style.boxShadow = "0 2px 6px rgba(0,0,0,0.1)";
        card.style.transition = "opacity 0.3s ease-in-out";

        // ✨ Add song info
        card.innerHTML = `
      <strong>${song.title}</strong> by ${song.artist}<br>
      Album: ${song.album} (${song.year})<br>
    `;

        // ✨ Add image with lazy loading + fade-in + fallback
        if (song.image_s3_url) {
            const img = document.createElement("img");
            img.src = song.image_s3_url;

            img.alt = song.title;
            img.loading = "lazy"; // ✅ Lazy loading
            img.classList.add("song-image");
            img.style.width = "150px";
            img.style.marginTop = "10px";
            img.style.borderRadius = "6px";
            img.style.objectFit = "cover";
            img.style.opacity = "0";
            img.style.transition = "opacity 0.5s ease-in-out";

            // ✅ Fade-in after loading
            img.onload = () => {
                img.style.opacity = "1";
            };

            // ⚠️ Fallback if image fails
            img.onerror = function() {
                this.onerror = null;
                this.src = "no-image.jpg";
            };
            console.log("🎯 Image URL for", song.title, ":", song.image_s3_url);


            card.appendChild(img);
        }

        card.setAttribute("data-key", key); // ✅ for selective updates

        if (subscribedTitles.includes(key)) {
            const label = document.createElement("p");
            label.classList.add("subscribed-label"); // ✅ For targeting
            label.style.color = "green";
            label.style.fontWeight = "bold";
            label.innerText = "✅ Already Subscribed";
            card.appendChild(label);
        } else {
            const button = document.createElement("button");
            button.innerText = "Subscribe";
            button.addEventListener("click", () => {
                subscribe(song.title, song.album);
            });
            card.appendChild(button);
        }

        container.appendChild(card);
    });
}




async function subscribe(title, album) {
    //console.log("subscribe() called", { title, album, stack: new Error().stack });

    if (isSubscribing) return;
    isSubscribing = true;

    const email = localStorage.getItem("email");

    const messageBox = document.getElementById("subscribeMessage"); // You need to add this element

    if (!email) {
        showMessage("❌ User not logged in", true);
        isSubscribing = false;
        return;
    }

    const titleAlbumKey = `${title}#${album}`;
    if (subscribedSet.has(titleAlbumKey)) {
        showMessage("⚠️ You're already subscribed to this song!", true);
        isSubscribing = false;
        return;
    }

    try {
        showMessage("⏳ Subscribing to song...", false);

        const response = await fetch(subscribeEndpoint, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, title, album })
        });

        const raw = await response.json();
        console.log("🎯 Subscribe Lambda Raw Response:", raw);

        let data;
        try {
            data = raw?.body ? (typeof raw.body === "string" ? JSON.parse(raw.body) : raw.body) : raw;
        } catch (err) {
            console.error("❌ Failed to parse Lambda response:", err);
            data = { status: "fail", message: "Invalid response from server." };
        }

        if (data.status === "success") {
            showMessage("✅ Subscribed successfully!");
            subscribedSet.add(titleAlbumKey);

            await fetchSubscriptions(email);
            // ✅ Fetch latest subscription list
            const updatedSubscribedTitles = await fetchSubscriptions(email);

            // ✅ Re-render using cached search results
            if (window.lastSearchResults) {
                renderSearchResults(window.lastSearchResults, updatedSubscribedTitles);
            }
        } else {
            showMessage(data.message || "❌ Subscription failed.", true);
        }

    } catch (err) {
        console.error("❌ Error subscribing:", err);
        showMessage("❌ Something went wrong while subscribing.", true);
    } finally {
        isSubscribing = false;
    }

    // ✅ Inline message display helper
    function showMessage(msg, isError = true) {
        if (!messageBox) return;
        messageBox.innerText = msg;
        messageBox.style.color = isError ? "red" : "green";
        messageBox.style.fontWeight = "bold";
    }
}


function logout() {
    const confirmLogout = confirm("Are you sure you want to logout?");
    if (confirmLogout) {
        localStorage.clear();
        document.body.innerHTML = "<h2>👋 You’ve been logged out. Redirecting to login...</h2>";
        setTimeout(() => {
            window.location.href = "login.html";
        }, 1500); // Delay for 1.5 seconds
    }
}
