Vue.use(window.vuelidate.default)
const { required, email, numeric } = window.validators

window.addEventListener("load", function (event) {
    new Vue({
        el: '#main',
        data: {
            loading: false,
            email: '',
            userFilters: {
                product: '',
                provider: '',
                errorCode: '',
                customerId: '',
                fromCreationDate: '',
                untilCreationDate: '',
                fromLastModifiedDate: '',
                untilLastModifiedDate: '',
                status: '',
            },
            cases: [],
            columns: ['#', 'Case ID', 'Error code', 'Provider', 'Product', 'Created', 'Last modified', 'Status', 'Resource'],
        },
        validations: {
            userFilters: {
                errorCode: { numeric },
                customerId: { numeric },
                provider: { numeric },
            },
        },

        methods: {
            checkIsValid (val, event) {
                if (val.$error) {
                    event.target.classList.add('form__input-shake')
                    setTimeout(() => {
                        event.target.classList.remove('form__input-shake')
                    }, 600)
               }
            },
            searchBtn: function (e) {
                e.preventDefault();
                if (!this.$v.$invalid) {
                    this.fetchData();
                }
            },
            clearBtn: function (event) {
                this.userFilters.product = '';
                this.userFilters.provider = '';
                this.userFilters.errorCode = '';
                this.userFilters.customerId = '';
                this.userFilters.fromCreationDate = '';
                this.userFilters.untilCreationDate = '';
                this.userFilters.fromLastModifiedDate = '';
                this.userFilters.untilLastModifiedDate = '';
                this.userFilters.status = '';
                this.fetchData();
            },
            refresh: function (event) {
                this.loading = true;
                axios
                    .get('/refresh')
                this.fetchData();
            },
            deleteData: function (event) {
                this.loading = true;
                axios
                    .get('/delete')
                this.fetchData();
            },
            fetchData: function (e) {
                this.loading = true;
                axios
                    .get('/search?product=' + this.userFilters.product + '&provider=' + this.userFilters.provider + '&errorCode='
                        + this.userFilters.errorCode + '&customerId=' + this.userFilters.customerId + '&fromCreationDate='
                        + this.userFilters.fromCreationDate + '&untilCreationDate=' + this.userFilters.untilCreationDate + '&fromLastModifiedDate='
                        + this.userFilters.fromLastModifiedDate + '&untilLastModifiedDate=' + this.userFilters.untilLastModifiedDate
                        + '&status=' + this.userFilters.status)

                    .then((response) => {
                        this.cases = response.data
                    })
                    .finally(() => {
                        this.loading =  false
                    });
            }
        },
        mounted() {
            this.fetchData();
        },
    })
});
