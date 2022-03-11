#include <unordered_map>
#include <unordered_set>
#include <map>
#include <set>
#include <algorithm>
#include <cstdio>
#include <vector>
#include <string>
#include <cmath>
#include <ctime>
#include <omp.h>

using namespace std;

#define SUBSTR(s) {\
    s.pop_back(); \
    s.erase(0, 1); \
}

struct ServerType {
    string name;
    int core, mem, hardCost, energyCost;
    double confRatio;
};
struct VMType {
    int core, mem, v;
    double confRatio;
    int weight;
};
struct VM;
struct Server;
struct VM {
    VMType *type;
    Server *server;
    int id, part, migrated;
    int deleted;
};
struct Server{
    ServerType *type;
    int core[2], mem[2], id, nowPart;
    unordered_set<VM*> vms;
    int vmCnt;

    int singleLowCpu;
    int singleLowMem;

    // bool canDeployA;
    // bool canDeployB;
    // bool canDeployDouble;

    Server(ServerType *_type) {
        this->id = -1;
        this->type = _type;
        this->core[0] = this->core[1] = _type->core / 2;
        this->mem[0] = this->mem[1] = _type->mem / 2;
        this->nowPart = 0;
        this->vmCnt = 0;

        this->singleLowCpu = this->core[0];
        this->singleLowMem = this->mem[0];

        // this->canDeployA = true;
        // this->canDeployB = true;
        // this->canDeployDouble = true;
    }
};
struct OP {
    int a, id;
    string name;
};
struct MINFO {
    int id, server_id, part;
};
struct cmp_ptr {
    bool operator()(const VM* v1, const VM* v2) const {
        int sCnt1 = v1->server->vmCnt;
        int sCnt2 = v2->server->vmCnt;
        if (sCnt1 != sCnt2) {
            return sCnt1 < sCnt2;
        }

        int weight1 = v1->type->weight;
        int weight2 = v2->type->weight;
        if (weight1 != weight2) {
            return weight1 < weight2;
        }

        return v1->id < v2->id;
    }
};


class Manager {
public:
    Manager() {
        n = m = t = k = 0;
        totalCost = totalEnergyCost = totalHardCost = totalMigrateCnt = 0;
        ServerId = 0;
    }
    int n, m, t, k;
    int totalEnergyCost, totalHardCost, totalMigrateCnt, totalCost;
    int ServerId;

    int onPrint;

    vector<ServerType*> serverTypes;
    unordered_map<string, VMType*> vmTypes;
    
    vector<Server*> servers;
    unordered_map<int, VM*> vms;
    unordered_map<string, vector<Server*>> serverMap;
    vector<string> serverMapKeys;
    
    vector<vector<OP>> dayOpList;
    vector<Server*> todayPurchaseInfo;
    vector<VM*> todayDepInfo;
    vector<MINFO> todayMigrateInfo;

    vector<VM*> vmAdd;
    set<VM*, cmp_ptr> sortedVMs;
    unordered_set<Server*> updatedServers;
    vector<VM*> removedVMs;
    
    char *fileName;
    double runSeconds;

    void setFile(char *file) {
        fileName = file;
        freopen(file, "r", stdin);
    }

    void setPrint(int flag) {
        onPrint = flag;
    }

    void readDataFromFile() {
        scanf("%d", &n);
        char str[100];
        string name;
        for (int i = 0; i < n; ++i) {
            scanf("%s", str);
            name = string(str);
            SUBSTR(name);
            ServerType *s = new ServerType;
            s->name = name;
            scanf("%d, %d, %d, %d)", &s->core, &s->mem, &s->hardCost, &s->energyCost);
            s->confRatio = s->core * 1.0 / s->mem;
            //if (s->core < 128 || s->mem < 128) continue;
            serverTypes.push_back(s);
        }

        vector<VMType*> vmTypeArr;
        scanf("%d", &m);
        for (int i = 0; i < m; ++i) {
            scanf("%s", str);
            name = string(str);
            SUBSTR(name);
            VMType *vmType = new VMType;
            scanf("%d, %d, %d)", &vmType->core, &vmType->mem, &vmType->v);
            vmType->confRatio = vmType->core * 1.0 / vmType->mem;
            vmTypes[name] = vmType;
            vmTypeArr.push_back(vmType);
        }
        sort(vmTypeArr.begin(), vmTypeArr.end(), [] (VMType *&vt1, VMType *&vt2) {
            int score1 = vt1->core * 5 + vt2->mem * 4;
            int score2 = vt2->core * 5 + vt2->mem * 4;
            return score1 > score2;
        });
        for (int i = 0; i < vmTypeArr.size(); ++i) {
            vmTypeArr[i]->weight = i;
        }

        scanf("%d", &t);
        scanf("%d", &k);
    }

    void readDataContinue() {
        char str[100];
        string name;
        int r, a, id;
        dayOpList.push_back(vector<OP>());
        scanf("%d", &r);
        while (r--) {
            scanf("%s", str);
            if (str[1] == 'a') {
                a = 0;
                scanf("%s", str);
                name = string(str);
                name.pop_back();
                scanf("%d)", &id);
            } else {
                a = 1;
                name = "";
                scanf("%d)", &id);
            }
            dayOpList[dayOpList.size() - 1].push_back((OP) { a, id, name });
        }
    }

    void migrate() {
        int migrationNum = vms.size() * 3 / 100;
        if (migrationNum == 0) {
            return;
        }

        vector<Server*> tempServers;
        bool f1, f2, f3, f4, f5, f6, f7;
        for (int i = 0; i < servers.size(); ++i) {
            Server *s = servers[i];
            f1 = (s->core[0] == 0) && (s->core[1] == 0);
            f2 = (s->core[0] == 0) && (s->mem[1] == 0);
            f3 = (s->mem[0] == 0) && (s->core[1] == 0);
            f4 = (s->mem[0] == 0) && (s->mem[1] == 0);
            f5 = (s->vms.size() == 0);
            f6 = (s->core[0] + s->core[1]) < s->type->core * 0.02;
            f7 = (s->mem[0] + s->mem[1]) < s->type->mem * 0.02;
            if (f1 || f2 || f3 || f4 || f5 || f6 || f7) continue;
            if (s->vms.size() > 1) {
                tempServers.push_back(s);
            }
            unordered_set<VM*> vmList = s->vms;
            for (auto it : vmList) {
                VM *vm = it;
                it->migrated = 1;
            }
        }

        int successCnt = 0, failCnt = 0;
        for (auto it = sortedVMs.begin(); it != sortedVMs.end(); ++it) {
            VM *vm = *it;
            if (vm->deleted) {
                continue;
            }
            if (!vm->migrated) continue;
            vm->migrated = 0;
            VMType *vmt = vm->type;
            Server *fromServer = vm->server;
            if (successCnt >= migrationNum) {
                break;
            }
            if (failCnt >= 1000) {
                break;
            }

            int f = 0, idx = -1;
            int score1 = keepScore1(vm);
            int score2 = keepScore2(vm);

            #pragma omp parallel for num_threads(2)
            for (int j = 0; j < tempServers.size(); ++j) {
                if (canDeploy(vm->type, tempServers[j])) {
                    if (tempServers[j] == fromServer) {
                       continue;
                    }
                    int newScore1 = deployScore1(vm->type, tempServers[j]);
                    int newScore2 = deployScore2(vm->type, tempServers[j]);
                    if (newScore1 < score1) {
                        f = 1;
                        score1 = newScore1;
                        score2 = newScore2;
                        idx = j;
                    } else if (newScore1 == score1 && newScore2 < score2) {
                        f = 1;
                        score2 = newScore2;
                        idx = j;
                    }
                    if (score1 <= 0 || score2 <= 0) continue;
                }
            }

            if (f) {
                successCnt++;
                totalMigrateCnt++;
                failCnt = 0;

                Server *toServer = tempServers[idx];
                fromServer->vms.erase(vm);
                toServer->vms.insert(vm);
                vm->server = toServer;
                updatedServers.insert(toServer);
                updatedServers.insert(fromServer);
                if (vmt->v == 0) {
                    fromServer->core[vm->part] += vmt->core;
                    fromServer->mem[vm->part] += vmt->mem;
                    deployPart(vm, toServer);
                } else {
                    for (int j = 0; j < 2; ++j) {
                        fromServer->core[j] += vmt->core >> 1;
                        fromServer->mem[j] += vmt->mem >> 1;
                        toServer->core[j] -= vmt->core >> 1;
                        toServer->mem[j] -= vmt->mem >> 1;
                    }
                    // fromServer->canDeployDouble = true;
                    // if(toServer->core[0] == 0 || toServer->core[1] == 0 || toServer->mem[0] == 0 || toServer->mem[1] == 0)
                    //     toServer->canDeployDouble = false;
                }
                toServer->singleLowCpu = min(toServer->core[0],toServer->core[1]);
                toServer->singleLowMem = min(toServer->mem[0],toServer->mem[1]);
                fromServer->singleLowCpu = min(fromServer->core[0],fromServer->core[1]);
                fromServer->singleLowMem = min(fromServer->mem[0],fromServer->mem[1]);



                todayMigrateInfo.push_back((MINFO) {
                    vm->id, toServer->id, vm->part
                });
            } else {
                failCnt++;
            }
        }
    }

    Server* purchaseByVMT(VMType *vmt) {
        ServerType *st;
        for (int i = 0; i < serverTypes.size(); ++i) {
            st = serverTypes[i];
            if (canBuy(vmt, st)) {
                return purchase(st);
            }
        }

        return nullptr;
    }

    Server* purchase(ServerType *st) {
        Server *s = new Server(st);
        servers.push_back(s);
        todayPurchaseInfo.push_back(s);
        return s;
    }

    int canBuy(VMType *vmt, ServerType *st) {
        if (vmt->v == 0) {
            if (st->core >= 2 * vmt->core && st->mem >= 2 * vmt->mem) {
                return 1;
            }
        } else {
            if (st->core >= vmt->core && st->mem >= vmt->mem) {
                return 1;
            }
        }
        return 0;
    }

    int canDeploy(VMType *vmt, Server *server) {
        if (vmt->v == 0) {
            if ((server->core[0] >= vmt->core && server->mem[0] >= vmt->mem) || (server->core[1] >= vmt->core && server->mem[1] >= vmt->mem)) {
                return 1;
            }
        } else {
            if (server->singleLowCpu >= vmt->core >> 1 && server->singleLowMem >= vmt->mem >> 1) {
                return 1;
            }
        }
        return 0;
    }

    int deployScore1(VMType *vmt, Server *server) {
        int leftCore, leftMem;
        if (vmt->v == 0) {
            if (server->nowPart == 0) {
                if (server->core[0] >= vmt->core && server->mem[0] >= vmt->mem) {
                    leftCore = server->core[0] - vmt->core;
                    leftMem = server->mem[0] - vmt->mem;
                } else {
                    leftCore = server->core[1] - vmt->core;
                    leftMem = server->mem[1] - vmt->mem;
                }
            } else {
                if (server->core[1] >= vmt->core && server->mem[1] >= vmt->mem) {
                    leftCore = server->core[1] - vmt->core;
                    leftMem = server->mem[1] - vmt->mem;
                } else {
                    leftCore = server->core[0] - vmt->core;
                    leftMem = server->mem[0] - vmt->mem;
                }
            }
        } else {
            leftCore = server->core[0] + server->core[1] - vmt->core;
            leftMem = server->mem[0] + server->mem[1] - vmt->mem;
        }
        return leftCore * 5 + leftMem * 4;
    }

    int deployScore2(VMType *vmt, Server *server) {
        int leftCore, leftMem;
        if (vmt->v == 0) {
            if (server->nowPart == 0) {
                if (server->core[0] >= vmt->core && server->mem[0] >= vmt->mem) {
                    leftCore = server->core[0] - vmt->core;
                    leftMem = server->mem[0] - vmt->mem;
                } else {
                    leftCore = server->core[1] - vmt->core;
                    leftMem = server->mem[1] - vmt->mem;
                }
            } else {
                if (server->core[1] >= vmt->core && server->mem[1] >= vmt->mem) {
                    leftCore = server->core[1] - vmt->core;
                    leftMem = server->mem[1] - vmt->mem;
                } else {
                    leftCore = server->core[0] - vmt->core;
                    leftMem = server->mem[0] - vmt->mem;
                }
            }
            return abs(leftCore - leftMem);
        } else {
            int highCore = max(server->core[0], server->core[1]);
            int highMem = max(server->mem[0], server->mem[1]);
            leftCore = highCore - vmt->core / 2;
            leftMem = highMem - vmt->mem / 2;
            return leftCore + leftMem;
        }
    }

    int keepScore1(VM *vm) {
        VMType *vmt = vm->type;
        Server *server = vm->server;
        int leftCore, leftMem;
        if (vmt->v == 0) {
            if (server->nowPart == 0) {
                if (server->core[0] >= vmt->core && server->mem[0] >= vmt->mem) {
                    leftCore = server->core[0];
                    leftMem = server->mem[0];
                } else {
                    leftCore = server->core[1];
                    leftMem = server->mem[1];
                }
            } else {
                if (server->core[1] >= vmt->core && server->mem[1] >= vmt->mem) {
                    leftCore = server->core[1];
                    leftMem = server->mem[1];
                } else {
                    leftCore = server->core[0];
                    leftMem = server->mem[0];
                }
            }
        } else {
            leftCore = server->core[0] + server->core[1];
            leftMem = server->mem[0] + server->mem[1];
        }
        return leftCore * 4 + leftMem * 2 - min(leftCore, leftMem);
        //return leftCore * 5 + leftMem * 4 - max(leftCore, leftMem);
    }

    int keepScore2(VM *vm) {
        VMType *vmt = vm->type;
        Server *server = vm->server;
        int leftCore, leftMem;
        if (vmt->v == 0) {
            if (server->nowPart == 0) {
                if (server->core[0] >= vmt->core && server->mem[0] >= vmt->mem) {
                    leftCore = server->core[0];
                    leftMem = server->mem[0];
                } else {
                    leftCore = server->core[1];
                    leftMem = server->mem[1];
                }
            } else {
                if (server->core[1] >= vmt->core && server->mem[1] >= vmt->mem) {
                    leftCore = server->core[1];
                    leftMem = server->mem[1];
                } else {
                    leftCore = server->core[0];
                    leftMem = server->mem[0];
                }
            }
            return abs(leftCore - leftMem);
        } else {
            int highCore = max(server->core[0], server->core[1]);
            int highMem = max(server->mem[0], server->mem[1]);
            leftCore = highCore;
            leftMem = highMem;
            return leftCore + leftMem;
        }
    }

    void deployPart(VM *vm, Server *server) {
        if (server->nowPart == 0) {
            if (server->core[0] >= vm->type->core && server->mem[0] >= vm->type->mem) {
                server->core[0] -= vm->type->core;
                server->mem[0] -= vm->type->mem;
                vm->part = 0;
               // if (server->core[0] == 0 || server->mem[0] == 0) server->canDeployA = false;
            } else {
                server->core[1] -= vm->type->core;
                server->mem[1] -= vm->type->mem;
                vm->part = 1;
              //  if (server->core[1] == 0 || server->mem[1] == 0) server->canDeployB = false;
            }
        } else {
            if (server->core[1] >= vm->type->core && server->mem[1] >= vm->type->mem) {
                server->core[1] -= vm->type->core;
                server->mem[1] -= vm->type->mem;
                vm->part = 1;
              //  if (server->core[1] == 0 || server->mem[1] == 0) server->canDeployB = false;
            } else {
                server->core[0] -= vm->type->core;
                server->mem[0] -= vm->type->mem;
                vm->part = 0;
              //  if (server->core[0] == 0 || server->mem[0] == 0) server->canDeployA = false;
            }
        }

        int ascore = 0, bscore = 0;
        ascore += (server->core[0] + server->mem[0]);
        bscore += (server->core[1] + server->mem[1]);

        if (ascore >= bscore) {
            server->nowPart = 0;
        } else {
            server->nowPart = 1;
        }
    }

    void deploy(VM *vm, Server *server) {
        if (vm->type->v == 0) {
            deployPart(vm, server);
        } else {
            for (int i = 0; i < 2; ++i) {
                server->core[i] -= vm->type->core / 2;
                server->mem[i] -= vm->type->mem / 2;
            }
            // if(server->core[0] == 0 || server->core[1] == 0 || server->mem[0] == 0 || server->mem[1] == 0)
            //     server->canDeployDouble = false;
        }
        server->singleLowCpu = min(server->core[0],server->core[1]);
        server->singleLowMem = min(server->mem[0],server->mem[1]);
        
        vm->server = server;
        server->vms.insert(vm);
        updatedServers.insert(server);
    }

    int tryDeploy(VM *vm) {
        Server *server = nullptr;
        int score1 = 1e9, score2 = 1e9;
        for (auto it : servers) {
            Server *s = it;
            if (canDeploy(vm->type, it)) {
                int newScore1 = deployScore1(vm->type, s);
                int newScore2 = deployScore2(vm->type, s);
                if (newScore1 < score1) {
                    score1 = newScore1;
                    score2 = newScore2;
                    server = s;
                } else if (newScore1 == score1 && newScore2 < score2) {
                    score2 = newScore2;
                    server = s;
                }
            }
        }

        if (server != nullptr) {
            deploy(vm, server);
            return 1;
        }

        return 0;
    }

    void remove(VM *vm) {
        VMType *vmt = vm->type;
        Server *server = vm->server;

        if (vmt->v == 0) {
            server->core[vm->part] += vmt->core;
            server->mem[vm->part] += vmt->mem;
            int ascore = 0, bscore = 0;
            ascore += server->core[0] + server->mem[0];
            bscore += server->core[1] + server->mem[1];

            // if(vm->part == 0) server->canDeployA = true;
            // else server->canDeployB = true;

            if (ascore >= bscore) {
                server->nowPart = 0;
            } else {
                server->nowPart = 1;
            }
        } else {
            for (int i = 0; i < 2; ++i) {
                server->core[i] += vmt->core >> 1;
                server->mem[i] += vmt->mem >> 1;
            }
          //  server->canDeployDouble = true;
        }
        server->singleLowCpu = min(server->core[0],server->core[1]);
        server->singleLowMem = min(server->mem[0],server->mem[1]);

        server->vms.erase(vm);
        vms.erase(vm->id);
        vm->deleted = 1;
        updatedServers.insert(server);
        removedVMs.push_back(vm);
        // delete vm;
    }

    void sortServerTypes(int d) {
        int leftDay = t - d;
        sort(serverTypes.begin(), serverTypes.end(), [leftDay] (ServerType *&s1, ServerType *&s2) {
            return s1->hardCost + leftDay * s1->energyCost < s2->hardCost + leftDay * s2->energyCost;
        });
    }

    void deployVMAdd() {
        sort(vmAdd.begin(), vmAdd.end(), [] (VM *&v1, VM *&v2) {
            VMType *s1 = v1->type, *s2 = v2->type;
            int ascore = 0, bscore = 0;
            ascore += s1->core + s1->mem;
            bscore += s2->core + s2->mem;
            return ascore > bscore;
        });
        for (int i = 0; i < vmAdd.size(); ++i) {
            VM *vm = vmAdd[i];
            int f = tryDeploy(vm);
            if (!f) {
                Server *server = purchaseByVMT(vm->type);
                deploy(vm, server);
            }
        }
        vmAdd.clear();
    }

    void updateSortedVMs() {
        // TODO
        for (auto it : removedVMs) {
            int num = sortedVMs.erase(it);
            if (num == 0) {
                // printf("err\n");
            }
        }
        for (auto it : updatedServers) {
            unordered_set<VM*> vmList = it->vms;
            for (auto iter : vmList) {
                sortedVMs.erase(iter);
            }
        }
        for (auto it : updatedServers) {
            it->vmCnt = it->vms.size();
        }
        for (auto it : updatedServers) {
            unordered_set<VM*> vmList = it->vms;
            for (auto iter : vmList) {
                sortedVMs.insert(iter);
            }
        }
    }

    void run() {
        time_t timeStart = clock();
        readDataFromFile();
        for (int d = 0; d < t; ++d) {
            readDataContinue();
            migrate();
            sortServerTypes(d);

            vector<OP> opList = dayOpList[d];
            for (int i = 0; i < opList.size(); ++i){
                int id = opList[i].id;
                string name = opList[i].name;
                if (opList[i].a == 0){
                    VM *vm = new VM;
                    vm->id = id;
                    vm->type = vmTypes[name];
                    vm->part = -1;
                    vm->migrated = 0;
                    vm->deleted = 0;
                    vms[vm->id] = vm;
                    vmAdd.push_back(vm);
                    todayDepInfo.push_back(vm);
                } else {
                    VM *vm = vms[id];
                    deployVMAdd();
                    remove(vm);
                }
            }
            deployVMAdd();
            updateSortedVMs();
            
            calCost();
            printTodayInfo();
            todayPurchaseInfo.clear();
            todayMigrateInfo.clear();
            todayDepInfo.clear();
            updatedServers.clear();
            removedVMs.clear();
        }

        time_t timeEnd = clock();
        runSeconds = (timeEnd - timeStart) * 1.0 / CLOCKS_PER_SEC;
    }

    void printResult() {
        printf("\n");
        printf("运行文件：%s\n", fileName);
        printf("迁移次数：%d\n", totalMigrateCnt);
        printf("物理机购买数量：%d\n", servers.size());
        printf("物理机购买成本：%d\n", totalHardCost);
        printf("物理机运行成本：%d\n", totalEnergyCost);
        printf("运行时间：%.2f\n", runSeconds);
        printf("总成本：%d\n", totalCost);
        printf("\n");
    }

    void printTodayInfo() {
        unordered_map<string, int> purchaseCounter;
        for (int i = 0; i < todayPurchaseInfo.size(); ++i) {
            Server *s = todayPurchaseInfo[i];
            if (purchaseCounter.count(s->type->name)) {
                purchaseCounter[s->type->name]++;
            } else {
                purchaseCounter[s->type->name] = 1;
            }
        }
        if (onPrint) printf("(purchase, %d)\n", purchaseCounter.size());
        for (auto it = purchaseCounter.begin(); it != purchaseCounter.end(); ++it) {
            if (onPrint) printf("(%s, %d)\n", it->first.c_str(), it->second);
            for (int i = 0; i < todayPurchaseInfo.size(); ++i) {
                Server *s = todayPurchaseInfo[i];
                if (it->first == s->type->name) {
                    s->id = ServerId++;
                }
            }
        }

        if (onPrint) printf("(migration, %d)\n", todayMigrateInfo.size());
        for (int i = 0; i < todayMigrateInfo.size(); ++i) {
            MINFO minfo = todayMigrateInfo[i];
            if (minfo.part != -1) {
                if (onPrint) printf("(%d, %d, %c)\n", minfo.id, minfo.server_id, minfo.part == 0 ? 'A' : 'B');
            } else {
                if (onPrint) printf("(%d, %d)\n", minfo.id, minfo.server_id);
            }
        }

        for (int i = 0; i < todayDepInfo.size(); ++i) {
            VM *vm = todayDepInfo[i];
            if (vm->part != -1) {
                if (onPrint) printf("(%d, %c)\n", vm->server->id, vm->part == 0 ? 'A' : 'B');
            } else {
                if (onPrint) printf("(%d)\n", vm->server->id);
            }
        }
    }

    void calCost() {
        for (int i = 0; i < servers.size(); ++i) {
            Server *s = servers[i];
            if (s->vms.size() > 0) {
                totalEnergyCost += s->type->energyCost;
            }
        }
        int sum = 0;
        for (int i = 0; i < servers.size(); ++i) {
            Server *s = servers[i];
            sum += s->type->hardCost;
        }
        totalHardCost = sum;
        totalCost = totalHardCost + totalEnergyCost;
    }
};

int main() {
    // Manager manager;
    // manager.setPrint(1);
    // manager.run();

    Manager manager1;
    manager1.setFile("huaweiRT2/training-1.txt");
    manager1.setPrint(0);
    manager1.run();
    manager1.printResult();

    Manager manager2;
    manager2.setFile("huaweiRT2/training-2.txt");
    manager2.setPrint(0);
    manager2.run();
    manager2.printResult();

    printf("测试集运行时间：%.2f\n", manager1.runSeconds + manager2.runSeconds);
    printf("测试集迁移总数：%d\n", manager1.totalMigrateCnt + manager2.totalMigrateCnt);
    printf("测试集成本总和：%d\n", manager1.totalCost + manager2.totalCost);

    return 0;
}
