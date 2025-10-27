import fetch from 'node-fetch';

async function testAuth() {
  const baseUrl = 'http://localhost:3000/api/auth';
  
  try {
    console.log('Testing user registration...');
    
    // Test registration
    const registerResponse = await fetch(`${baseUrl}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: 'John Doe',
        email: 'jane@gmail.com',
        password: 'SecurePass123@',
        date_of_birth: '1990-01-01'
      })
    });
    
    const registerResult = await registerResponse.json();
    console.log('Registration result:', registerResult);
    
    if (registerResult.success) {
      console.log('\n✅ Registration successful!');
      console.log('Token:', registerResult.token);
      
      // Test login
      console.log('\nTesting user login...');
      const loginResponse = await fetch(`${baseUrl}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: 'jane@gmail.com',
          password: 'SecurePass123@'
        })
      });
      
      const loginResult = await loginResponse.json();
      console.log('Login result:', loginResult);
      
      if (loginResult.success) {
        console.log('\n✅ Login successful!');
        console.log('User:', loginResult.user);
      } else {
        console.log('\n❌ Login failed:', loginResult.message);
      }
    } else {
      console.log('\n❌ Registration failed:', registerResult.message);
    }
    
  } catch (error) {
    console.error('Test failed:', error);
  }
}

testAuth();