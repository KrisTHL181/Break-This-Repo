// #pragma once

// #include <chrono>
// #include <ratio>
// #include <iostream>
// #include <string>
// #include <unordered_map>
// #include <stack>

// namespace ServerProfiler {
//     using clock = std::chrono::high_resolution_clock;

//     struct Entry {
//         std::string name;
//         clock::time_point start;
//     };

//     static std::stack<Entry> stack;
//     static std::unordered_map<std::string, double> times;

//     inline void push(const std::string& name) {
//         stack.push({name, clock::now()});
//     }

//     inline void pop() {
//         auto end = clock::now();
//         auto e = stack.top();
//         stack.pop();

//         double dt = std::chrono::duration<double, std::micro>(end - e.start).count();
//         times[e.name] += dt;
//     }

//     inline void popPush(const std::string& name) {
//         pop(); push(name);
//     }

//     inline void report() {
//         for (auto& it : times) {
//             std::cout << "[PROFILER] " << it.first << ": " << it.second << std::endl;
//         }
//     }
// };