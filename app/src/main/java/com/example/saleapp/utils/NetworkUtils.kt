package com.example.saleapp.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket

/**
 * Utility class for network operations and server discovery
 */
object NetworkUtils {
    private const val TAG = "NetworkUtils"
    private const val PREFS_NAME = "server_prefs"
    private const val KEY_SERVER_IP = "server_ip"
    private const val DEFAULT_PORT = 5000
    private const val SCAN_TIMEOUT_MS = 3000L
    
    // Fallback IP address to use if discovery fails
    private const val FALLBACK_SERVER_IP = "192.168.1.38"
    
    /**
     * Get the server IP address. Will first check for cached IP, then
     * automatically discover the server, falling back to the default IP if needed.
     */
    suspend fun getServerIpAddress(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Check if we have a cached IP address
        val cachedIp = prefs.getString(KEY_SERVER_IP, null)
        if (!cachedIp.isNullOrEmpty() && isServerReachable(cachedIp)) {
            Log.d(TAG, "Using cached server IP: $cachedIp")
            return cachedIp
        }
        
        // If no cached IP or it's not reachable, try to discover the server
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting server discovery...")
                val discoveredIp = discoverServer(context)
                
                if (discoveredIp != null) {
                    // Save the discovered IP
                    prefs.edit().putString(KEY_SERVER_IP, discoveredIp).apply()
                    Log.d(TAG, "Server discovered at: $discoveredIp")
                    discoveredIp
                } else {
                    Log.w(TAG, "Server discovery failed, using fallback IP: $FALLBACK_SERVER_IP")
                    // Save the fallback IP for faster future connections
                    prefs.edit().putString(KEY_SERVER_IP, FALLBACK_SERVER_IP).apply()
                    FALLBACK_SERVER_IP
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during server discovery: ${e.message}")
                // Save the fallback IP for faster future connections
                prefs.edit().putString(KEY_SERVER_IP, FALLBACK_SERVER_IP).apply()
                FALLBACK_SERVER_IP
            }
        }
    }
    
    /**
     * Get the server port number
     */
    fun getServerPort(): Int {
        return DEFAULT_PORT
    }
    
    /**
     * Check if the server is reachable at the given IP address
     */
    private suspend fun isServerReachable(ipAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                val inetAddress = InetAddress.getByName(ipAddress)
                socket.connect(java.net.InetSocketAddress(inetAddress, DEFAULT_PORT), 1000)
                socket.close()
                true
            } catch (e: Exception) {
                Log.d(TAG, "Server at $ipAddress is not reachable: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Discover the server on the local network
     */
    private suspend fun discoverServer(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Get the WiFi subnet
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val dhcpInfo = wifiManager.dhcpInfo
                if (dhcpInfo == null) {
                    Log.e(TAG, "Cannot get DHCP info, WiFi may not be connected")
                    return@withContext null
                }
                
                // Convert int IP to String
                val ipAddress = intToIpAddress(dhcpInfo.ipAddress)
                val subnet = ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1)
                
                Log.d(TAG, "Local device IP: $ipAddress, scanning subnet: $subnet*")
                
                // First try the fallback IP - it's likely to be correct in many cases
                if (isServerRunningAt(FALLBACK_SERVER_IP)) {
                    Log.d(TAG, "Server found at fallback IP: $FALLBACK_SERVER_IP")
                    return@withContext FALLBACK_SERVER_IP
                }
                
                // Try common server IPs first (often used for servers)
                val commonIPs = listOf("1", "2", "100", "200", "254")
                for (ip in commonIPs) {
                    val host = subnet + ip
                    if (host != ipAddress && isServerRunningAt(host)) {
                        Log.d(TAG, "Server found at common IP: $host")
                        return@withContext host
                    }
                }
                
                // Scan a limited range to save time and resources
                for (i in 1..20) {
                    val host = subnet + i.toString()
                    if (host == ipAddress) continue
                    
                    if (isServerRunningAt(host)) {
                        Log.d(TAG, "Server found during initial scan: $host")
                        return@withContext host
                    }
                }
                
                // If server not found in limited scan, try a more comprehensive scan
                for (i in 21..255) {
                    val host = subnet + i.toString()
                    if (host == ipAddress) continue
                    
                    if (isServerRunningAt(host)) {
                        Log.d(TAG, "Server found during comprehensive scan: $host")
                        return@withContext host
                    }
                }
                
                Log.d(TAG, "Server not found on the network")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error during network scan: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Check if our server is running at the given host
     */
    private fun isServerRunningAt(host: String): Boolean {
        return try {
            val socket = Socket()
            socket.connect(java.net.InetSocketAddress(host, DEFAULT_PORT), SCAN_TIMEOUT_MS.toInt())
            
            // If connection succeeds, try a simple handshake
            val outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()
            
            // Send a simple ping
            outputStream.write("""{"PaymentType":"Ping"}""".toByteArray())
            outputStream.flush()
            
            // Read response (with timeout)
            socket.soTimeout = 1000
            val buffer = ByteArray(128)
            val bytesRead = inputStream.read(buffer)
            
            socket.close()
            
            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                // Check if the response looks like it's from our server
                response.contains("ResponseCode") || response.contains("PaymentType")
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Convert integer IP address to string format
     */
    private fun intToIpAddress(ipAddress: Int): String {
        return "${ipAddress and 0xff}.${ipAddress shr 8 and 0xff}.${ipAddress shr 16 and 0xff}.${ipAddress shr 24 and 0xff}"
    }
} 